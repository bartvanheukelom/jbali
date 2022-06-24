package org.jbali.jmsrpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.jbali.json.fromJson2
import org.jbali.json.toJson2
import org.jbali.kotser.Transformer
import org.jbali.kotser.jsonSerializer
import org.jbali.memory.Borrowed
import org.jbali.memory.loan
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.text.toMessageString
import org.jbali.util.cast
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod


fun interface DeepMemberPointer<TContainer, TMember> {
    /**
     * Must always be invoked with the same [container], or one that is equivalent.
     */
   operator fun invoke(container: TContainer): TMember
}

internal class TMSInterface<I : Any>(
    bIface: Borrowed<KClass<I>>
) {
    companion object {
        private val log = LoggerFactory.getLogger(TMSInterface::class.java)
    }
    
    class TMethod(
        // we can't store the KFunction because we only Borrowed the interface
        // TODO oh noooo... the KSerializers probably do keep references to their classes. can we weakref them?
        val method: DeepMemberPointer<KClass<*>, KFunction<*>>,
        val params: List<TParam>,
        val returnSerializer: TMSSerializer,
    ) {
        val paramsByName = params.associateBy { it.name }
    }
    class TParam(
        val param: DeepMemberPointer<KFunction<*>, KParameter>,
        val name: String,
        val serializer: TMSSerializer,
    )
    
    val name = bIface().qualifiedName
    
    override fun toString() = "TMSInterface(name=$name)"
    
    val methods: Map<String, TMethod> = run {
    
        val iface = bIface()
        val warningLoggedFor = mutableSetOf<KClass<*>>()
        fun KClass<*>.ifaceKose(): Boolean = when {
            this.hasAnnotation<KoSe>() -> true
            this.hasAnnotation<JJS>()  -> false
            else -> {
                if (warningLoggedFor.add(this)) {
                    log.warn("$iface is not annoted with KoSe or JJS, and will currently default to JJS, but in the future will default to KoSe")
                }
                false
            }
        }
        
        val memFunPointers: List<DeepMemberPointer<KClass<*>, KFunction<*>>> =
            iface.memberFunctions.indices.map { i ->
                DeepMemberPointer { ifa: KClass<*> ->
                    // TODO not optimal because memberFunctions is a filtering getter
                    ifa.memberFunctions
                        .cast<List<KFunction<*>>>()[i]
                }
            }
        
        memFunPointers
            .filter {
                it(iface).javaMethod!!.declaringClass != Object::class.java
            }
            .groupBy { it(iface).name }
            .mapValues { (n, f) ->
                if (f.size == 1) f.single()
                else throw IllegalArgumentException(f.toMessageString(
                    "Encountered duplicate methods. TextMessageService does not support overloads."
                ))
            }
            .mapValues { (n, fp) ->
                val func = fp(iface)
                try {
                    val methodKose = when {
                        func.hasAnnotation<KoSe>() -> true
                        func.hasAnnotation<JJS >() -> false
                        else                       -> func.javaMethod!!.declaringClass.kotlin.ifaceKose() // TODO what's the kotlin equivalent for declaringClass?
                    }
                    val returnKose = when {
                        func.hasAnnotation<KoSeReturn>() -> true
                        func.hasAnnotation<JJSReturn >() -> false
                        else                             -> methodKose
                    }
                    
                    val params = func.parameters
                        .drop(1) // this
                        .mapIndexed { i, p ->
                            try {
                                val paramSer = when (val koa = p.findAnnotation<KoSe>()) {
                                    null ->
                                        if (!methodKose || p.hasAnnotation<JJS>()) {
                                            JjsAsTms
                                        } else {
                                            serializer(p.type).asTms()
                                        }
                                    else ->
                                        @Suppress("UNCHECKED_CAST")
                                        when (val w = koa.with) {
                                            KSerializer::class -> serializer(p.type)
                                            else -> koa.with.objectInstance as KSerializer<Any?> // TODO good message e.g. if not object but class
                                        }.asTms()
                                }
                                TParam(
                                    param = { f -> f.parameters[i + 1] },
                                    name = p.name ?: throw IllegalArgumentException("Method has unsupported param $p"),
                                    serializer = paramSer,
                                )
                            } catch (e: Throwable) {
                                throw RuntimeException("For parameter ${p.name}: $e", e)
                            }
                        }
                    
                    val returnSer = if (returnKose) {
                        func.returnType.let(::serializer).asTms()
                    } else {
                        JjsAsTms
                    }
        
                    TMethod(
                        method = fp,
                        params = params,
                        returnSerializer = returnSer,
                    )
                } catch (e: Exception) {
                    throw RuntimeException("For function ${func.name}: $e", e)
                }
            }
    }
    
    val methodsLowerName: Map<String, TMethod> = methods
        .mapKeys { it.key.lowercase() }
    
    val methodsByJavaMethod: Map<Method, TMethod> = methods
        .values.associateByTo(WeakHashMap()) {
            it.method(bIface()).javaMethod
        }
    
}


internal val <I : Any> KClass<I>.asTMSInterface: TMSInterface<I>
    get() = TMSInterface(this.loan())
// TODO disabled because probably leaks, see comment in TMSInterface
//        by StoredExtensionProperty {
//            TMSInterface(this)
//        }

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
