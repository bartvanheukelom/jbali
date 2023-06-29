@file:OptIn(ExperimentalTime::class)

package org.jbali.jmsrpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.jbali.json.fromJson2
import org.jbali.json.toJson2
import org.jbali.kotser.Transformer
import org.jbali.kotser.jsonSerializer
import org.jbali.kotser.std.UUIDSerializer
import org.jbali.memory.Borrowed
import org.jbali.memory.loan
import org.jbali.serialize.JavaJsonSerializer
import org.jbali.text.showing
import org.jbali.text.toMessageString
import org.jbali.util.Box
import org.jbali.util.cast
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.typeOf
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration


fun interface DeepMemberPointer<TContainer, TMember> {
    /**
     * Must always be invoked with the same [container], or one that is equivalent.
     */
   operator fun invoke(container: TContainer): TMember
}

class TMSInterface<I : Any>(
    bIface: Borrowed<KClass<I>>
) {
    companion object {
        private val log = LoggerFactory.getLogger(TMSInterface::class.java)
    }
    
    class TMethod(
        val name: String,
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
    
    /**
     * Keyed by lowercase method name.
     */
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
        
        /*methods=*/ memFunPointers
            
            // omit toString(), hashCode(), etc.
            .filter {
                it(iface).javaMethod!!.declaringClass != Object::class.java
            }
            
            // associate by lowercase name
            // TODO make associateByUnique/Distinct
            .groupBy { it(iface).name.lowercase() }
            .mapValues { (n, f) ->
                if (f.size == 1) f.single()
                else throw IllegalArgumentException(f.toMessageString(
                    "Encountered duplicate methods. TextMessageService does not support overloads, or method names that are different only in case."
                ))
            }
            
            .mapValues { (_, fp) ->
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
                        val rt = func.returnType
                        if ("$rt".endsWith("!")) { // TODO find better way
                            // TODO make error when have time to fix it all
                            log.warn("Method $func return type does not declare nullability, i.e. is a platform type. This may lead to issues finding a serializer and will be an error in the future.")
                            
                            // nice try but the whole problem is that this annotation is not retained at runtime
//                            if (func.hasAnnotation<NotNull>()) {
//                                log.warn("Method was annotated with @org.jetbrains.annotations.NotNull. Use @javax.annotation.Nonnull to get the desired effect.")
//                            }
                        }
                        when (rt) {
                            typeOf<Unit>() -> UnitTMSSerializer
                            
                            // TODO is this clean? in any case, allow specifying custom serializers somehow.
                            // TODO in any case, duplicating the logic for nullable or not is not good. similarly, List<UUID> etc should also work.
                            typeOf<UUID >() -> UUIDSerializer         .asTms()
                            typeOf<UUID?>() -> UUIDSerializer.nullable.asTms()
                            
                            else -> rt.let(::serializer).asTms()
                        }
                    } else {
                        check(func.returnType != typeOf<Unit?>()) {
                            "JavaJsonSerializer cannot be used for `Unit?` as it cannot distinguish between Unit and null"
                        }
                        JjsAsTms
                    }
        
                    TMethod(
                        name = func.name,
                        method = fp,
                        params = params,
                        returnSerializer = returnSer,
                    )
                } catch (e: Exception) {
                    throw RuntimeException("For function ${func.name}: $e", e)
                }
            }
    }
    
    init {
        log.info("$this methods" showing methods.values.map { m ->
            "${m.name}(${m.params.joinToString { p -> p.name }})"
        })
    }
    
}


val <I : Any> KClass<I>.asTMSInterface: TMSInterface<I>
    get() =
// TODO disabled because probably leaks, see comment in TMSInterface
//        by StoredExtensionProperty {
            initInterface(this@asTMSInterface)
//        }

private fun <I : Any> initInterface(iface: KClass<I>): TMSInterface<I> =
    measureTimedValue { TMSInterface(iface.loan()) }
        .also { TMSMeters.recordIfaceInit(it.duration.toJavaDuration(), it.value.name) }
        .value

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

/**
 * Lenient serializer for [Unit] which ignores the JSON value when unserializing, allowing a method to start returning
 * something, while remaining compatible with callers that expect Unit.
 */
internal object UnitTMSSerializer : TMSSerializer {
    private val ser = Unit.serializer().asTms()
    override fun transform(obj: Any?) = ser.transform(obj)
    override fun detransform(tf: JsonElement) = Unit
}

/**
 * Use this alias for [Box] to indicate that it's used to work around
 * a bug in [TextMessageService], e.g. to wrap an inline class.
 */
typealias TMSBox<T> = Box<T>
fun <T> T.tmsBoxed() = TMSBox(this)
