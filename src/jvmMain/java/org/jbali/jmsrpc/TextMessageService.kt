package org.jbali.jmsrpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jbali.errors.removeCurrentStack
import org.jbali.errors.stackTraceString
import org.jbali.json2.JSONString
import org.jbali.kotser.empty
import org.jbali.kotser.jsonString
import org.jbali.kotser.string
import org.jbali.kotser.toJsonElement
import org.jbali.reflect.callByWithBetterExceptions
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.util.onceFunction
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
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
    
    private val ifaceK = iface.kotlin
    private val ifaceInfo = ifaceK.asTMSInterface
    
    override fun handleRequest(request: String): String {

        var methName = "?"
        val logTheRequest = onceFunction { log.info("In text request $svcName.$methName:") }
        
        TMSMeters.startServerRequest(ifaceInfo.name).use { meter ->
    
            val response: JsonArray = try {
    
                // parse request json
                val reqJson = try {
                    JSONString(request).parse() as JsonArray
                } catch (e: Throwable) {
                    throw IllegalArgumentException("Could not parse request", e)
                }
    
                // determine method
                methName = reqJson[RQIDX_METHOD].string
                meter.methodName = methName
                val method = ifaceInfo.methods[methName.lowercase()]
                    ?: throw NoSuchElementException("Unknown method '$methName'")
                val func = method.method(ifaceK)
                
                // read arguments
                val inArgs = if (reqJson.size > RQIDX_ARGS) reqJson[RQIDX_ARGS].jsonObject else JsonObject.empty
                val pars = method.params
                val args = mutableMapOf<KParameter, Any?>(
                    func.instanceParameter!! to endpoint
                )
                inArgs.forEach { (name, serVal) ->
                    method.paramsByName[name]?.let { par ->
                        val kPar = par.param(func)
                        args[kPar] = par.serializer.detransform(serVal)
                    }
                    // TODO log a warning once, for each redundant arg
                }
    
                // execute
                val ret = try {
                    func.callByWithBetterExceptions(args)
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
    
                // serialize response
                val serRet = try {
                    method.returnSerializer.transform(ret)
                } catch (e: Throwable) {
                    log.warn("${e.javaClass.name} while serializing return value $ret")
                    throw RuntimeException("Exception serializing return value of type ${ret?.javaClass?.name} (see log for contents): $e", e)
                }
                
                // return response
                meter.success = true
                JsonArray(listOf(
                    STATUS_OK.toJsonElement(),
                    serRet,
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
                meter.success = false
    
                try {
                    JsonArray(listOf(
                        STATUS_ERROR.toJsonElement(),
                        JjsAsTms.transform(e),
                        jsonString(e.stackTraceString),
                        // TODO also return toString, in case the client can't deserialize the exception class.
                        // TODO transform exception cause chain into list and serialize each exception individually.
                    ))
                } catch (serEr: Throwable) {
                    serEr.removeCurrentStack()
                    log.warn("!! Error while serializing error", serEr)
                    try {
                        JsonArray(listOf(
                            STATUS_ERROR.toJsonElement(),
                            JjsAsTms.transform(RuntimeException("$e [exception class could not be serialized: $serEr]")),
                            jsonString(e.stackTraceString),
                        ))
                    } catch (serErEr: Throwable) {
                        serErEr.removeCurrentStack()
                        log.warn("!! Error while getting exception toString()", serErEr)
                        JsonArray(listOf(
                            STATUS_ERROR.toJsonElement(),
                            JjsAsTms.transform(RuntimeException("Error occurred but could not be serialized (see server log for details)")),
                            jsonString(""),
                        ))
                    }
                }
    
            }
    
            return try {
                JSONString.stringify(response, prettyPrint = false).string
            } catch (e: Throwable) {
                log.warn("Error toStringing JSON response", e)
                meter.success = false
                "[0, null]"
            }
        }

    }

    companion object {

        const val RQIDX_METHOD = 0
        const val RQIDX_ARGS = 1

        const val RSIDX_STATUS = 0
        const val RSIDX_RESPONSE = 1

        const val STATUS_OK = 1
        const val STATUS_ERROR = 0
    
        inline fun <reified T : Any> testWrap(endpoint: T) = testWrap(T::class.java, endpoint)
        
        fun <T : Any> testWrap(
            iface: Class<out T>,
            endpoint: T
        ): TextMessageServiceClient<T> =
            TextMessageServiceClient(
                iface.kotlin,
                requestHandler = TextMessageService(iface, endpoint = endpoint)::handleRequest
            )
        
    }

}

/**
 * Apply to interfaces, methods or parameters in a [TextMessageService] interface that should use kotlinx.serialization.
 * @param with Serializer to use for the annotated _parameter_.
 */
annotation class KoSe(
    val with: KClass<out KSerializer<*>> = KSerializer::class // Default value indicates that auto-generated serializer is used
)
/**
 * Apply to methods in a [TextMessageService] interface that should use kotlinx.serialization _for the return value_.
 */
annotation class KoSeReturn(
//    val with: KClass<out KSerializer<*>> = KSerializer::class // Default value indicates that auto-generated serializer is used
)

/**
 * Apply to interfaces, methods or parameters in a [TextMessageService] interface that should use the legacy [JavaJsonSerializer].
 */
annotation class JJS
/**
 * Apply to methods in a [TextMessageService] interface that should use use the legacy [JavaJsonSerializer] _for the return value_.
 */
annotation class JJSReturn
