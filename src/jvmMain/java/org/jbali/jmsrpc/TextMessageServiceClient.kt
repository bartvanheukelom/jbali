package org.jbali.jmsrpc

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import org.jbali.coroutines.Suspending
import org.jbali.json2.JSONString
import org.jbali.kotser.toJsonElement
import org.jbali.kotser.unwrap
import org.jbali.reflect.Proxies
import org.jbali.reflect.kClassOrNull
import org.jbali.serialize.JavaJsonSerializer
import java.util.function.Function
import kotlin.reflect.KClass

class TextMessageServiceClient<S : Any>(
    private val ifaceK: KClass<out S>,
    requestHandler: (String) -> String,
) : AutoCloseable {
    
    companion object {
    
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
        
        try {
            
            Proxies.handleTEH(proxy, method, args, "${toStringed}.blocking")
                ?.right() // toString, equals or hashCode
                ?: run {
                    
                    // --- ok, it's a real method --- //
                    
                    val tMethod = ifaceInfo.methodsByJavaMethod[method]
                        ?: throw NoSuchElementException("'$method' not found in $ifaceInfo. Methods with the same name:\n\t${
                            ifaceInfo.methodsByJavaMethod.keys
                                .filter { it.name == method.name }
                                .joinToString("\n\t")
                        }")
                    val func = tMethod.method(ifaceK)
                    
                    // serialize the invocation to JSON
                    // TODO send args object instead of array
                    val reqJson = buildJsonArray {
                        add(method.name.toJsonElement())
                        
                        args?.asSequence()
                            ?.mapIndexed { p, arg ->
                                val par = tMethod.params[p]
                                val kpar = par.param(func)
                                try {
                                    par.serializer.transform(arg)
                                } catch (e: Exception) {
                                    throw RuntimeException("Error serializing arg of type ${arg.kClassOrNull} for $kpar.name: $e", e)
                                }
                            }
                            ?.forEach(::add)
                    }
                    
                    // send the request
                    val respJson = requestHandler(JSONString.stringify(reqJson, prettyPrint = false).string)
                    
                    // parse the response
                    val respParsed = JSONString(respJson).parse() as JsonArray
                    val respStatus = (respParsed[TextMessageService.RSIDX_STATUS].unwrap() as Double).toInt()
                    val respJsonEl = respParsed[TextMessageService.RSIDX_RESPONSE]
                    
                    // return or throw it
                    when (respStatus) {
                        TextMessageService.STATUS_OK -> {
                            tMethod.returnSerializer
                                .detransform(respJsonEl)
                                .right()
                        }
                        else ->
                            // the response should be an exception
                            JjsAsTms
                                .detransform(respJsonEl)
                                .let { it as? Throwable ?: error("Service returned an error that is not Throwable but ${it?.javaClass}") }
                                
                                // add the local stack trace to the remote exception,
                                // otherwise that info is lost - unless we wrap the exception in a new local one,
                                // which we don't want because it breaks the remote API.
                                .also { augmentStackTrace(
                                    err = it,
                                    // discard traces like:
                                    //     at org.jbali.jmsrpc.TextMessageServiceClient.create$lambda-6(TextMessageServiceClient.kt:106)
                                    //     at com.sun.proxy.$Proxy8.${ifaceMethodThatWasInvoked}(Unknown Source)
                                    discard = 2
                                ) }
                                
                                .left()
                    }
                    
                }
            
        } catch (e: Throwable) {
            throw TextMessageServiceClientException("A local/meta exception occured when invoking $toStringed.${method.name}: $e", e)
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
