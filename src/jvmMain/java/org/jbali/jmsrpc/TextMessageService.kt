package org.jbali.jmsrpc

import org.jbali.errors.removeCurrentStack
import org.jbali.json.JSONArray
import org.jbali.reflect.Methods
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.util.onceFunction
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

private val log = LoggerFactory.getLogger(TextMessageService::class.java)!!

// TODO simple kotlin-serialization JSON instead of JavaJsonSerializer.
//      perhaps migrate to that slowly with an annotation per method.
class TextMessageService<T : Any>(
        private val iface: Class<out T>,
        private val svcName: String = iface.name,
        private val endpoint: T
) {

    private val methods: Map<String, Method> =
            Methods.mapPublicMethodsByName(iface)
                .mapKeys { it.key.toLowerCase() }

    fun handleRequest(request: String): String {

        var methName = "?"
        val logTheRequest = onceFunction { log.info("In text request $svcName.$methName:") }

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

            // read arguments
            val pars = method.parameters

            val args = method.parameters.mapIndexed { p, par ->
                val indexInReq = p + 1
                if (reqJson.length() < indexInReq + 1) {
                    // this parameter has no argument
                    logTheRequest()
                    log.info("- Arg #" + p + " (" + par.type + " " + par.name + ") omitted")
                    null // let's hope that's sufficient
                } else {
                    JavaJsonSerializer.unserialize(reqJson.get(indexInReq))
                }
            }.toTypedArray()

            // check for more args than used
            val extraArgs = reqJson.length() - 1 - pars.size
            if (extraArgs > 0) {
                logTheRequest()
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
            e.removeCurrentStack()

            logTheRequest()
            log.warn("Error handling request", e)

            try {
                JSONArray.create(STATUS_ERROR, JavaJsonSerializer.serialize(e))!!
            } catch (serEr: Throwable) {
                serEr.removeCurrentStack()
                log.warn("!! Error while serializing error", serEr)
                JSONArray.create(STATUS_ERROR, JavaJsonSerializer.serialize(RuntimeException("Error occurred but could not be serialized (see log for details)")))!!
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

        const val RQIDX_METHOD = 0

        const val RSIDX_STATUS = 0
        const val RSIDX_RESPONSE = 1

        const val STATUS_OK = 1
        const val STATUS_ERROR = 0

    }

}
