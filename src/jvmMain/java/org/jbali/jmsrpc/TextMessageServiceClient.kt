package org.jbali.jmsrpc

import arrow.core.Either
import arrow.core.getOrHandle
import org.jbali.json.JSONArray
import org.jbali.reflect.Proxies
import org.jbali.serialize.JavaJsonSerializer
import java.util.*
import java.util.function.Function

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

        return Proxies.create(iface) { proxy, method, args ->

            try {

                Proxies.handleTEH(proxy, method, args, toStringed)?.let { Either.Right(it) } ?: {

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
                    val respPayload = JavaJsonSerializer.unserialize(respParsed.get(TextMessageService.RSIDX_RESPONSE)) ?: null

                    // return or throw it
                    when (respStatus) {
                        TextMessageService.STATUS_OK -> Either.Right(respPayload)
                        else -> Either.Left(
                                // the response should be an exception
                                (respPayload as? Throwable ?: throw IllegalStateException("Service returned an error that is not Throwable but ${respPayload?.javaClass}"))
                                // add the local stack trace to the remote exception,
                                // otherwise that info is lost - unless we wrap the exception in a new local one,
                                // which we don't want because it breaks the remote API.
                                // 5 = discard lambdas, invocation handler and proxy method from trace
                                .also { augmentStackTrace(it, 5) }
                        )
                    }

                }()

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
