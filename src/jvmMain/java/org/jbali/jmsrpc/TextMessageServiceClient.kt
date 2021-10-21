package org.jbali.jmsrpc

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import io.ktor.serialization.*
import kotlinx.serialization.serializer
import org.jbali.json.JSONArray
import org.jbali.json.JSONObject
import org.jbali.reflect.Proxies
import org.jbali.reflect.kClassOrNull
import org.jbali.serialize.JavaJsonSerializer
import java.util.function.Function
import kotlin.reflect.jvm.kotlinFunction

object TextMessageServiceClient {

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
    fun <S> create(iface: Class<S>, requestHandler: Function<String, String>): S {

        val toStringed = "TextMessageServiceClient[" + iface.simpleName + "]"
        val ifaceKose = iface.isAnnotationPresent(KoSe::class.java)
    
        return Proxies.create(iface) { proxy, method, args ->

            try {

                Proxies.handleTEH(proxy, method, args, toStringed)
                    ?.right() // toString, equals or hashCode
                    ?: run {
    
                        // --- ok, it's a real method --- //
    
                        // which serialization to use
                        val methodKose = when {
                            method.isAnnotationPresent(KoSe::class.java) -> true
                            method.isAnnotationPresent(JJS ::class.java) -> false
                            else                                         -> ifaceKose
                        }
    
                        // serialize the invocation to JSON
                        val reqJson = JSONArray.create(method.name)!!
                        args?.asSequence()
                            ?.mapIndexed { p, arg ->
                                val par = method.parameters[p]!!
                                
                                val paramKose = when {
                                    par.isAnnotationPresent(KoSe::class.java) -> true
                                    par.isAnnotationPresent(JJS ::class.java) -> false
                                    else                                      -> methodKose
                                }

                                if (paramKose) {
                                    val kpar = method.kotlinFunction!!.parameters[p + 1]
                                    val argSer = kpar.type.let(::serializer) // TODO cache
                                    try {
                                        arg
                                            .let { DefaultJson.encodeToString(argSer, it) }  // TODO optimize, use kose json the whole way
                                            .let { JSONArray("[$it]").get(0) }
                                    } catch (e: Exception) {
                                        throw RuntimeException("Error serializing arg of type ${arg.kClassOrNull} for $kpar using serializer $argSer: $e", e)
                                    }
                                } else {
                                    JavaJsonSerializer.serialize(arg)
                                }
                            }
                            ?.forEach(reqJson::put)
    
                        // send the request
                        val respJson = requestHandler.apply(reqJson.toString(2))
    
                        // parse the response
                        val respParsed = JSONArray(respJson)
                        val respStatus = respParsed.getInt(TextMessageService.RSIDX_STATUS)
                        val respJsonEl = respParsed.get(TextMessageService.RSIDX_RESPONSE)
    
                        // return or throw it
                        when (respStatus) {
                            TextMessageService.STATUS_OK -> {
                                if (methodKose) {
                                    val argSer = method.kotlinFunction!!.returnType.let(::serializer) // TODO cache
                                    respJsonEl
                                        .let(JSONObject::valueToString) // TODO optimize, use kose json the whole way
                                        .let { DefaultJson.decodeFromString(argSer, it) }
                                } else {
                                    JavaJsonSerializer.unserialize(respJsonEl) ?: null // TODO document why ?: null
                                }.right()
                            }
                            else ->
                                // the response should be an exception
                                JavaJsonSerializer.unserialize(respJsonEl)
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

    }

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
