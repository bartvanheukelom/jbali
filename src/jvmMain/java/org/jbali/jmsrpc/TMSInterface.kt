package org.jbali.jmsrpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.jbali.json.fromJson2
import org.jbali.json.toJson2
import org.jbali.kotser.Transformer
import org.jbali.kotser.jsonSerializer
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.text.toMessageString
import org.jbali.util.Borrowed
import org.jbali.util.StoredExtensionProperty
import org.jbali.util.cast
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod


internal class TMSInterface<I : Any>(
    bIface: Borrowed<KClass<I>>
) {
    companion object {
        private val log = LoggerFactory.getLogger(TMSInterface::class.java)
    }
    
    class TMethod(
        val method: WeakReference<KFunction<*>>,
        val params: List<TParam>,
        val returnSerializer: TMSSerializer,
    )
    class TParam(
        val param: WeakReference<KParameter>,
        val serializer: TMSSerializer,
    )
    
    val methods: Map<String, TMethod> = run {
    
        val iface = bIface()
        val ifaceKose: Boolean = when {
            iface.hasAnnotation<KoSe>() -> true
            iface.hasAnnotation<JJS>()  -> false
            else -> {
                log.warn("$iface is not annoted with KoSe or JJS, and will currently default to JJS, but in the future will default to KoSe")
                false
            }
        }
        
        iface
            .memberFunctions
            .filter {
                it.javaMethod!!.declaringClass != Object::class.java
            }
            .groupBy { it.name }
            .mapValues { (n, f) ->
                if (f.size == 1) f.single()
                else throw IllegalArgumentException(f.toMessageString(
                    "Encountered duplicate methods. TextMessageService does not support overloads."
                ))
            }
            .mapValues { (n, func) ->
    
//                val func = method.kotlinFunction!!
                
                val methodKose = when {
                    func.hasAnnotation<KoSe>() -> true
                    func.hasAnnotation<JJS >() -> false
                    else                       -> ifaceKose
                }
                val returnKose = when {
                    func.hasAnnotation<KoSeReturn>() -> true
                    func.hasAnnotation<JJSReturn >() -> false
                    else                             -> methodKose
                }
                
                val params = func.parameters
                    .drop(1) // this
                    .map { p ->
                        val paramKose = when {
                            p.hasAnnotation<KoSe>() -> true
                            p.hasAnnotation<JJS >() -> false
                            else                    -> methodKose
                        }
                        TParam(
                            param = WeakReference(p),
                            serializer = if (paramKose) {
                                p.type.let(::serializer).asTms()
                            } else {
                                JjsAsTms
                            }
                        )
                    }
                
                val returnSer = if (returnKose) {
                    func.returnType.let(::serializer).asTms()
                } else {
                    JjsAsTms
                }
    
                TMethod(
                    method = WeakReference(func),
                    params = params,
                    returnSerializer = returnSer,
                )
            }
    }
    
    val methodsLowerName: Map<String, TMethod> = methods
        .mapKeys { it.key.lowercase() }
    
    val methodsByJavaMethod: Map<Method, TMethod> = methods
        .values.associateByTo(WeakHashMap()) {
            it.method.get()!!.javaMethod
        }
    
}


internal val <I : Any> KClass<I>.asTMSInterface: TMSInterface<I>
        by StoredExtensionProperty {
            TMSInterface(this)
        }

typealias TMSSerializer = Transformer<Any?, JsonElement>

private fun KSerializer<*>.asTms() = this
    .cast<KSerializer<Any?>>()
    .jsonSerializer()
    .elementTransformer()

internal object JjsAsTms : TMSSerializer {
    override fun transform(obj: Any?) =
        JavaJsonSerializer.serialize(obj).toJson2()
    
    override fun detransform(tf: JsonElement): Any? =
        JavaJsonSerializer.unserialize(tf.fromJson2())
    
}
