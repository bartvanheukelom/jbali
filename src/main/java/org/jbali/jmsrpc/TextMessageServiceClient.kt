package org.jbali.jmsrpc

import org.jbali.json.JSONArray
import org.jbali.reflect.Proxies
import org.jbali.serialize.JavaJsonSerializer
import java.util.*
import java.util.function.Function

object TextMessageServiceClient {

    private class RethrowException(cause: Throwable) : Exception(cause)

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

        return Proxies.create(iface) { proxy, method, args ->

            try {

                // catch local methods
                Proxies.handleObjectMethods(proxy, method, args, toStringed) ?: {

                    // --- ok, it's a real method --- //

                    // serialize the invocation to JSON
                    val reqJson = JSONArray.create(method.name)!!
                    args?.let {
                        Arrays.stream(it)
                                .map { JavaJsonSerializer.serialize(it) }
                                .forEach { reqJson.put(it) }
                    }

                    // send the request
                    val respJson = requestHandler.apply(reqJson.toString(2))

                    // parse the response
                    val respParsed = JSONArray(respJson)
                    val respStatus = respParsed.getInt(TextMessageService.RSIDX_STATUS)
                    val respPayload = JavaJsonSerializer.unserialize(respParsed.get(TextMessageService.RSIDX_RESPONSE))!!

                    // check for error
                    when (respStatus) {
                        TextMessageService.STATUS_OK -> respPayload
                        else -> throw RethrowException(
                                // the response should be an exception
                                makeSureIsThrowable(respPayload)
                                // add the local stack trace to the remote exception,
                                // otherwise that info is lost - unless we wrap the exception in a new local one,
                                // which we don't want because it breaks the remote API.
                                // 1 = discard invocation handler from trace
                                .also { augmentStackTrace(it, 1) }
                        )
                    }

                }

            } catch (e: RethrowException) {
                throw e.cause!!
            } catch (e: Throwable) {
                throw TextMessageServiceClientException("A local/meta exception occured when invoking $toStringed.${method.name}: $e", e)
            }


        }

    }

    private fun makeSureIsThrowable(err: Any?) = when(err) {
        null -> throw IllegalStateException("Service returned a null error")
        is Throwable -> err
        else -> throw IllegalStateException("Service returned an error that is not Throwable but " + err.javaClass)
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
