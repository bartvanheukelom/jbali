package org.jbali.jmsrpc

import org.jbali.json.JSONArray
import org.jbali.reflect.Methods
import org.jbali.serialize.JavaJsonSerializer
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

class TextMessageService(
        private val endpoint: Any
) {

    private val methods: Map<String, Method> =
            Methods.mapPublicMethodsByName(endpoint.javaClass)
                .mapKeys { it.key.toLowerCase() }

    fun handleRequest(request: String): String {

        var methName = "?"
        var className = endpoint.javaClass.name
        var requestLogged = false

        val response = try {

            // parse request json
            val reqJson = try {
                JSONArray(request)
            } catch (e: Throwable) {
                throw IllegalArgumentException("Could not parse request", e)
            }

            // determine method
            methName = reqJson.getString(RQIDX_METHOD)
            val method = methods[methName.toLowerCase()] ?: throw NoSuchElementException("Unknown method '$methName'")
            if (className != method.declaringClass.name)
                className += ">" + method.declaringClass.name

            // read arguments
            val pars = method.parameters

            val args = method.parameters.mapIndexed { p, par ->
                val indexInReq = p + 1
                if (reqJson.length() < indexInReq + 1) {
                    // this parameter has no argument
                    if (!requestLogged) {
                        log.info(methName + ":")
                        requestLogged = true
                    }
                    log.info("- Arg #" + p + " (" + par.type + " " + par.name + ") omitted")
                    null // let's hope that's sufficient
                } else {
                    JavaJsonSerializer.unserialize(reqJson.get(indexInReq))
                }
            }.toTypedArray()

            // check for more args than used
            val extraArgs = reqJson.length() - 1 - pars.size
            if (extraArgs > 0) {
                if (!requestLogged) {
                    log.info(methName + ":")
                    requestLogged = true
                }
                log.info("- $extraArgs args too many ignored.")
            }

            // execute
            val ret = try {
                method.invoke(endpoint, *args)
            } catch (e: InvocationTargetException) {
                // InvocationTargetException: actual exception inside method.
                throw e.cause!!
            } catch (e: ExceptionInInitializerError) {
                // ExceptionInInitializerError: always unchecked (initializers can't throw checked).
                throw e.cause!!
            } catch (e: IllegalAccessException) {
                // IllegalAccessException: method is public, should not happen.
                throw RuntimeException("TextMessageService internal error", e)
            } catch (e: NullPointerException) {
                // NullPointerException: endpoint is not null, should not happen.
                throw RuntimeException("TextMessageService internal error", e)
            }

            // return response
            JSONArray.create(STATUS_OK, JavaJsonSerializer.serialize(ret))!!

        } catch (e: Throwable) {

            // remove the current stack trace from the error stacktraces,
            // because it's not relevant to the client or logs.
            // TODO remove up to the endpoint as well, e.g.:
            //			at com.blabla.DataServer$RemoteDataServerImpl.authByXId(DataServer.java:123) ~[dataserver.jar:na]
            //			...
            //			at org.jbali.jmsrpc.TextMessageService.handleRequest(TextMessageService.java:95) ~[bali.jar:na]
            val locTrace = Thread.currentThread().stackTrace
            if (locTrace.size >= 3) { // you never know on some VMs
                val caller = locTrace[2]
                val thisMethod = locTrace[1]

                var toClean: Throwable? = e
                while (toClean != null) {
                    val errTrace = toClean.stackTrace

                    // find the calling method in the exception stack
                    for (i in errTrace.size - 1 downTo 1) {
                        if (errTrace[i] == caller) {
                            // check if the calling method did in fact call this method
                            // (line number won't match)
                            val nextCall = errTrace[i - 1]
                            if (nextCall.className == thisMethod.className && nextCall.methodName == thisMethod.methodName) {
                                // snip!
                                toClean.stackTrace = Arrays.copyOfRange(errTrace, 0, i)
                                break
                            }
                        }
                    }
                    toClean = toClean.cause
                }
            }

            log.warn("Error in text request for method $className.$methName", e)

            try {
                JSONArray.create(STATUS_ERROR, JavaJsonSerializer.serialize(e))!!
            } catch (serEr: Throwable) {
                log.warn("Error while serializing error", serEr)
                JSONArray.create(STATUS_ERROR, JavaJsonSerializer.serialize(RuntimeException("Error occurred but could not be serialized")))!!
            }

        }

        return try {
            response.toString(2)
        } catch (e: Throwable) {
            log.warn("Error toStringing JSON response", e)
            "[0, null]"
        }

    }

    companion object {

        val RQIDX_METHOD = 0

        val RSIDX_STATUS = 0
        val RSIDX_RESPONSE = 1

        val STATUS_OK = 1
        val STATUS_ERROR = 0

        private val log = LoggerFactory.getLogger(TextMessageService::class.java)
    }

}
