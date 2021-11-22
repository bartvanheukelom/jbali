package org.jbali.jmsrpc

import kotlinx.serialization.json.JsonArray
import org.jbali.errors.removeCurrentStack
import org.jbali.json2.JSONString
import org.jbali.kotser.string
import org.jbali.kotser.toJsonElement
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.util.onceFunction
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter

private val log = LoggerFactory.getLogger(TextMessageService::class.java)!!

interface ITextMessageService<T : Any> {
    fun handleRequest(request: String): String
}

@OptIn(ExperimentalStdlibApi::class)
class TextMessageService<T : Any>(
        iface: Class<out T>,
        private val svcName: String = iface.name,
        private val endpoint: T
) : ITextMessageService<T> {
    
    private val ifaceInfo = iface.kotlin.asTMSInterface
    
    override fun handleRequest(request: String): String {

        var methName = "?"
        val logTheRequest = onceFunction { log.info("In text request $svcName.$methName:") }

        val response: JsonArray = try {

            // parse request json
            val reqJson = try {
                JSONString(request).parse() as JsonArray
            } catch (e: Throwable) {
                throw IllegalArgumentException("Could not parse request", e)
            }

            // determine method
            methName = reqJson[RQIDX_METHOD].string
            val method = ifaceInfo.methodsLowerName[methName.lowercase()]
                ?: throw NoSuchElementException("Unknown method '$methName'")
            val func = method.method.get()!!
            
            // read arguments
            // TODO support reading from object instead of array
            val pars = method.params
            val args = mutableMapOf<KParameter, Any?>(
                func.instanceParameter!! to endpoint
            )
            pars.forEachIndexed { p, par ->
                val kPar = par.param.get()!!
                val indexInReq = p + 1
                if (reqJson.size < indexInReq + 1) {
                    // this parameter has no argument
//                    logTheRequest()
//                    log.info("- Arg ${par.name} omitted")
                    if (kPar.isOptional) {
                        // not putting it in args, will result in the default value being used
                    } else {
                        // let's hope this is good enough
                        args[kPar] = null
                    }
                } else {
                    val serVal = reqJson[indexInReq]
                    args[kPar] = par.serializer.detransform(serVal)
                }
            }

            // check for more args than used
//            val extraArgs = reqJson.size - 1 - pars.size
//            if (extraArgs > 0) {
//                logTheRequest()
//                log.info("- $extraArgs args too many ignored.")
//            }

            // execute
            val ret = try {
                func.callBy(args)
                    // "void" KFunction returns Unit, which should be returned as null
                    .takeIf { it !is Unit }
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
            JsonArray(listOf(
                STATUS_OK.toJsonElement(),
                method.returnSerializer.transform(ret),
            ))

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
                JsonArray(listOf(
                    STATUS_ERROR.toJsonElement(),
                    JjsAsTms.transform(e),
                ))
            } catch (serEr: Throwable) {
                serEr.removeCurrentStack()
                log.warn("!! Error while serializing error", serEr)
                JsonArray(listOf(
                    STATUS_ERROR.toJsonElement(),
                    JjsAsTms.transform(RuntimeException("Error occurred but could not be serialized (see log for details)")),
                ))
            }

        }

        return try {
            JSONString.stringify(response, prettyPrint = false).string
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

/**
 * Apply to interfaces, methods or parameters in a [TextMessageService] interface that should use kotlinx.serialization.
 */
annotation class KoSe
/**
 * Apply to methods in a [TextMessageService] interface that should use kotlinx.serialization _for the return value_.
 */
annotation class KoSeReturn

/**
 * Apply to interfaces, methods or parameters in a [TextMessageService] interface that should use the legacy [JavaJsonSerializer].
 */
annotation class JJS
/**
 * Apply to methods in a [TextMessageService] interface that should use use the legacy [JavaJsonSerializer] _for the return value_.
 */
annotation class JJSReturn
