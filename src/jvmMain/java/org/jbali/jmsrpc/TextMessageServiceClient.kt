package org.jbali.jmsrpc

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import org.jbali.coroutines.Suspending
import org.jbali.coroutines.runBlockingInterruptable
import org.jbali.json2.JSONString
import org.jbali.kotser.toJsonElement
import org.jbali.kotser.toJsonObject
import org.jbali.kotser.unwrap
import org.jbali.reflect.Proxies
import org.jbali.reflect.kClassOrNull
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.util.MicroTime
import org.jbali.util.diffUIntClampedTo
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

class TextMessageServiceClient<S : Any>(
    private val ifaceK: KClass<out S>,
    blockRequestHandler: ((String) -> String)? = null,
    coroRequestHandler: (suspend (String) -> String)? = null,
) : AutoCloseable {
    
    constructor(
        ifaceK: KClass<out S>,
        requestHandler: (String) -> String,
    ) : this(ifaceK, blockRequestHandler = requestHandler)
    
    init {
        require(blockRequestHandler != null || coroRequestHandler != null) {
            "Must provide at least one of blockRequestHandler or coroRequestHandler"
        }
    }
    
    companion object {
        
        private val log = LoggerFactory.getLogger(TextMessageServiceClient::class.java)
    
        /**
         * Create an implementation of the given interface that will:
         *
         *  * Serialize invocations of its methods<sup>1</sup> to JSON using [JavaJsonSerializer].
         *  * Pass that JSON to the given `requestHandler`, which is presumed to send it to some (remote) service, and return the string response.
         *  * Parse the string response and return it, or if the response is an exception, throw it.
         *
         * <sup>1</sup>: The following methods are executed locally, so not passed to `requestHandler`:
         *
         *  * *toString*: returns "TextMessageServiceClient[" + iface.getSimpleName() + "]"
         *  * *equals*: Uses ==
         *  * *hashCode*: Uses System.identityHashCode()
         *
         */
        @JvmStatic
        fun <S : Any> create(iface: Class<out S>, requestHandler: Function<String, String>): S =
            TextMessageServiceClient(iface.kotlin, requestHandler::apply).blocking
    
//        fun <S : Any> create(iface: KClass<out S>, requestHandler: (String) -> String) =
//            TextMessageServiceClient(iface, requestHandler)
        
        inline fun <reified S : Any> create(noinline requestHandler: (String) -> String) =
            TextMessageServiceClient(S::class, requestHandler)
        
        /**
         * Append the current stack trace to the trace of the given exception.
         * @param discard The number of elements to discard
         */
        private fun augmentStackTrace(err: Throwable, discard: Int) {
    
            val remTrace:Array<StackTraceElement> = err.stackTrace
            val locTrace:Array<StackTraceElement> = Throwable().stackTrace
    
            // first the remote trace
            val complete = remTrace.toMutableList()
            // then a separator
            complete.add(StackTraceElement("==========================", "", "TextMessageService", -3))
            // then part of the local trace
            complete.addAll(locTrace.toList().subList(discard + 1, locTrace.size)) // +1 is augmentStackTrace
    
            err.stackTrace = complete.toTypedArray()
    
        }
    
    }
    
    
    private val toStringed = "TextMessageServiceClient[" + ifaceK.simpleName + "]"
    private val ifaceInfo = ifaceK.asTMSInterface
    
    val blocking: S = Proxies.create(ifaceK.java) { proxy, method, args ->
        
        var utStart: MicroTime? = MicroTime.now()
        fun record(success: Boolean) {
            if (utStart != null) {
                TMSMeters.recordClientRequest(
                    ifaceInfo.name, method.name, success,
                    Duration.ofMillis((utStart!! diffUIntClampedTo MicroTime.now()).toLong() / 1_000_000)
                )
                utStart = null
            }
        }
        
        TMSMeters.countRequestsActive.incrementAndGet()
        try {
            
            Proxies.handleTEH(proxy, method, args, "${toStringed}.blocking")
                ?.right() // toString, equals or hashCode
                ?: run {
                    
                    // --- ok, it's a real method --- //
                    
                    val tMethod = ifaceInfo.methods.getValue(method.name.lowercase())
                    val func = tMethod.method(ifaceK)
                    if (func.javaMethod != method) {
                        // TODO does this happen?
                        log.warn("Called '$method' is not equal to found '${func.javaMethod}'. Argument serialization may fail.")
                    }
                    
                    // serialize the invocation to JSON
                    val reqJson = buildJsonArray {
                        add(method.name.toJsonElement())
                        if (args != null) {
                            add(args.asSequence()
                                .mapIndexed { p, arg ->
                                    val par = tMethod.params[p]
                                    val kpar = par.param(func)
                                    try {
                                        par.name to par.serializer.transform(arg)
                                    } catch (e: Exception) {
                                        throw RuntimeException("Error serializing arg of type ${arg.kClassOrNull} for $kpar.name: $e", e)
                                    }
                                }
                                .toJsonObject()
                            )
                        }
                    }
                    
                    // send the request
                    val reqStr = JSONString.stringify(reqJson, prettyPrint = false).string
                    val respJson = when {
                        blockRequestHandler != null -> blockRequestHandler(reqStr)
                        coroRequestHandler != null -> runBlockingInterruptable { coroRequestHandler(reqStr) }
                        else -> throw IllegalStateException("Must provide at least one of blockRequestHandler or coroRequestHandler")
                    }
                    
                    // parse the response
                    val respParsed = JSONString(respJson).parse() as JsonArray
                    val respStatus = (respParsed[TextMessageService.RSIDX_STATUS].unwrap() as Double).toInt()
                    val respJsonEl = respParsed[TextMessageService.RSIDX_RESPONSE]
                    
                    // return or throw it
                    when (respStatus) {
                        TextMessageService.STATUS_OK -> {
                            record(true)
                            tMethod.returnSerializer
                                .detransform(respJsonEl)
                                .right()
                        }
                        else -> {
                            record(false)
                            // the response should be an exception
                            JjsAsTms
                                .detransform(respJsonEl)
                                
                                // add the local stack trace to the remote exception,
                                // otherwise that info is lost - unless we wrap the exception in a new local one,
                                // which we don't want because it breaks the remote API.
                                .also { if (it is Throwable) augmentStackTrace(
                                    err = it,
                                    // discard traces like:
                                    //     at org.jbali.jmsrpc.TextMessageServiceClient.create$lambda-6(TextMessageServiceClient.kt:106)
                                    //     at com.sun.proxy.$Proxy8.${ifaceMethodThatWasInvoked}(Unknown Source)
                                    discard = 2
                                ) }
                                
                                .let { when (it) {
                                    is TextMessageServiceClientException ->
                                        RuntimeException("Service returned the following exception, i.e. it wasn't generated locally: $it", it)
                                    is Throwable ->
                                        it
                                    else ->
                                        RuntimeException("Service returned an error that is not Throwable but ${it?.javaClass}")
                                } }
                                
                                .left()
                        }
                    }
                    
                }
            
        } catch (e: Throwable) {
            record(false)
            throw TextMessageServiceClientException("A local/meta exception occured when invoking $toStringed.${method.name}: $e", e)
        } finally {
            TMSMeters.countRequestsActive.decrementAndGet()
        }.getOrHandle { throw it }
        
    }
    
    val suspending = object : Suspending<S> {
        override suspend fun <R> invoke(call: S.() -> R): R =
            withContext(Dispatchers.IO) {
                blocking.call()
            }
    }
    
    override fun close() {
        // does nothing at the moment, but may in the future,
        // so start teaching users of this class to clean up after themselves now.
    }
    
}
