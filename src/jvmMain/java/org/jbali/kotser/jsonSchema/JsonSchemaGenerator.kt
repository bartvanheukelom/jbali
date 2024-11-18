@file:OptIn(ExperimentalSerializationApi::class)
package org.jbali.kotser.jsonSchema

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.jbali.json2.jsonQuote
import org.jbali.kotser.*
import org.jbali.reflect.isObject
import org.jbali.reflect.kClass
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

class JsonSchemaGenerator(
    private val pw: PrintWriter,
) : AutoCloseable {
    
    private val log = LoggerFactory.getLogger(javaClass)

    @Serializable
    object PH0
    @Serializable
    object PH1
    @Serializable
    object PH2
    @Serializable
    object PH3
    @Serializable
    object PH4
    @Serializable
    object PH5

    private val phs = listOf(
            typeOf<PH0>(),
            typeOf<PH1>(),
            typeOf<PH2>(),
            typeOf<PH3>(),
            typeOf<PH4>(),
            typeOf<PH5>(),
    )
    private val phNames = phs.map { it.classifier!! as KClass<*> }.map { it.qualifiedName!! }

    private var phReplaceNames: List<String>? = null



    private val known = mutableSetOf<KClass<*>>()
//    private val todo = mutableListOf<Pair<KClass<*>, Any?>>()
    private val todo = mutableListOf<KClass<*>>()

    private fun queue(type: KClass<*>) {
        if (known.add(type)) {
            todo.add(type)// to null)
            todo.sortBy { it.qualifiedName!! }
        }
    }

    inline fun <reified T> generate() {
        generate(T::class)
    }

    fun generate(
        type: KClass<*>,
//        strat:
    ) {
        queue(type)
        pw.processQueue()
    }

    private val nothing: Nothing get() = throw AssertionError()

    private var inNamespace: String = ""
    
    private fun PrintWriter.closeNs() {
        if (inNamespace != "") {
            println("}\n\n")
        }
        inNamespace = ""
    }
    
    private fun String.dequalify() = removePrefix(inNamespace + ".")

    private fun PrintWriter.processQueue() {
        while (todo.isNotEmpty()) {
            val clazz = todo.removeLast()

//            clazz.sealedSubclasses.forEach(::queue)
    
    
    
    
            // TODO instead of fake typing with placeholder, see constructSerializerForGivenTypeArgs
            val fakeType = clazz.createType(clazz.typeParameters.mapIndexed { i, tp ->
                KTypeProjection.invariant(phs[i])
            })
            phReplaceNames = clazz.typeParameters.map {
                it.name
            }
    
            val fakeSer = serializer(fakeType)
            val desc = fakeSer.descriptor
    
            val split = clazz.qualifiedName!!.split(".")
            val rn = split.last()
            val ns = split.subList(0, split.size - 1).joinToString(".")
//            println("// ns: $ns")
//            println("// inNamespace: $inNamespace")
            if (inNamespace != ns) {
//                println("// before close, inNamespace: $inNamespace")
                closeNs()
//                println("// after close, inNamespace: $inNamespace")
                println("export namespace $ns {")
                inNamespace = ns
//                println("// after inNamespace = ns, inNamespace: $inNamespace")
            } else {
//                println("// inNamespace unchanged: $inNamespace")
            }
    
            when {
                clazz.isValue -> {
                    val typeDef = desc.typeName()
//                    log.info("value class $clazz, desc $desc, typeDef $typeDef")
//                    log.info(desc.dump())
                    println("\n  export type $rn = $typeDef;")
                }
                clazz.isSubclassOf(Enum::class) -> {
                    val typeDef = clazz.java.enumConstants!!.joinToString(" | ") {
                        jsonString((it as Enum<*>).name).toString() // TODO SerialName
                    }
                    println("\n  export type $rn = $typeDef;")
                }
                desc.kind == PolymorphicKind.SEALED -> {
                    println("\n  export type $rn =");
                    val els = desc.elements
                    val valueDesc = els.single { it.name == "value" }.descriptor
                    for (e in valueDesc.elements) {
                        print("    | [${e.name.jsonQuote()}")
//                        log.info("${e.name} desc ${e.descriptor.dump()}")
                        if (e.descriptor.kind != StructureKind.OBJECT) {
                            print(", ${e.descriptor.typeName().dequalify()}")
                        }
                        println("]")
                    }
                    println("  ;")
                }
                else -> {
    
                    print("\n  export interface $rn")
    
                    if (clazz.typeParameters.isNotEmpty()) {
                        print(clazz.typeParameters.joinToString(prefix = "<", separator = ",", postfix = ">") {
                            buildString {
                                when (it.variance) {
                                    KVariance.INVARIANT -> {}
                                    KVariance.IN -> append("in ")
                                    KVariance.OUT -> append("out ")
                                }
                                append(it.name)
                                when {
                                    it.upperBounds == listOf(typeOf<Any?>()) -> {}
                                    it.upperBounds == listOf(typeOf<Any>()) -> append("extends any")
                                    it.upperBounds.size == 1 -> append("extends ${serializer(it.upperBounds.single()).descriptor.typeName()}")
                                    else -> append(" ::::: TODO ${it.upperBounds}")
                                }
                            }
                        })
                    }
                    println(" {")
    
                    for (e in 0 until desc.elementsCount) {
        
                        val eName = desc.getElementName(e)
                        val eDesc = desc.getElementDescriptor(e)
                        val eAnnot = desc.getElementAnnotations(e)
                        val eOpt = desc.isElementOptional(e)
        
                        if (eAnnot.isNotEmpty()) {
                            println("    // $eAnnot")
                        }
        
                        val tn = eDesc.typeName().dequalify()
                        println("    $eName${if (eOpt) "?" else ""}: $tn;")
                    }
    
                    println("  }")
    
                }
            }
            
        }
    }
    
    override fun close() {
        pw.closeNs()
    }

    private fun SerialDescriptor.typeName(): String =

            when (serialName) {

                "kotlin.Any" -> "any"
                "kotlin.Nothing" -> "nothing"

                "kotlin.Double",
                "kotlin.Long",
                "kotlin.Int",
                "kotlin.Short",
                "kotlin.Byte",
                "kotlin.UInt",
                "kotlin.UShort",
                "kotlin.UByte",
                "kotlin.ULong"
                    -> "number"

                "kotlin.String" -> "string"
                "kotlin.Boolean" -> "boolean"

                "kotlinx.serialization.LongAsStringSerializer" -> "LongAsString"
                "kotlinx.serialization.json.JsonElement" -> "JsonElement"
                "kotlinx.serialization.json.JsonObject" -> "JsonObject"
                "kotlinx.serialization.json.JsonArray" -> "JsonArray"
                "kotlinx.serialization.json.JsonPrimitive" -> "JsonPrimitive"

                "kotlin.collections.LinkedHashSet",
                "kotlin.collections.ArrayList" -> {
                    "${getElementDescriptor(0).typeName().dequalify()}[]"
                }

                "kotlin.collections.LinkedHashMap" -> {
                    "{ [key: ${getElementDescriptor(0).typeName().dequalify()}]: ${getElementDescriptor(1).typeName().dequalify()} }"
                }

                else -> {

                    when {

                        serialName in phNames -> {
                            phReplaceNames!![phNames.indexOf(serialName)]
                        }
                        
                        isInline -> {
                            elementDescriptors.single().typeName()
                        }

                        isNullable -> {
                            require(kClass.qualifiedName == "kotlinx.serialization.internal.SerialDescriptorForNullable")

                            val original = kClass.declaredMemberProperties
                                    .single { it.name == "original" }
                                    .also { it.isAccessible = true }
                                    .get(this) as SerialDescriptor
    
                            val orgType = original.typeName()
                            if (orgType.endsWith(" | null")) {
                                orgType
                            } else {
                                "$orgType | null"
                            }
                        }

                        kind == SerialKind.ENUM -> {
                            serialName
//                            // TODO typedef
//                            elementNames.joinToString(separator = " | ") {
//                                Json.Default.encodeToString(it)
//                            }
                        }

                        kind == SerialKind.CONTEXTUAL && serialName.startsWith("kotlinx.serialization.Sealed<") -> {
                            elementDescriptors.joinToString(separator = " | ") { it.typeName() }
                        }

                        kClass.simpleName == "PluginGeneratedSerialDescriptor" -> {
                            val generatedSerializer = kClass.declaredMemberProperties
                                    .single { it.name == "generatedSerializer" }
                                    .also { it.isAccessible = true }
                                    .get(this) as KSerializer<*>

                            val type = generatedSerializer.kClass.declaredMemberFunctions
                                    .single { it.name == "deserialize" }
                                    .returnType

                            val clazz = type.classifier as KClass<*>

                            if (clazz.isObject) {
                                "{}"
                            } else {

                                queue(clazz)

                                buildString {
                                    append(clazz.qualifiedName)
//                                    append("JSON")
                                    if (clazz.typeParameters.isNotEmpty()) {

                                        val typeParameterDescriptors = this@typeName.kClass.declaredMemberProperties
                                                .single { it.name == "typeParameterDescriptors" }
                                                .also { it.isAccessible = true }
                                                .get(this@typeName) as Array<SerialDescriptor>

                                        append(typeParameterDescriptors.joinToString(", ", "<", ">") {
                                            it.typeName()
                                        })
                                    }
                                }
                            }
                        }

                        else -> {
                            attemptQueueFromDesc()
                            serialName
                        }
                    }

                }
            }


    private fun SerialDescriptor.attemptQueueFromDesc() {
        try {
            queue(serialName.scanClassName())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    
    /**
     * Tries to find a class of this name, whose parts are initially separated by '.',
     * e.g. "com.example.MyClass.InnerThing".
     * If not found, recursively replaces the last '.' with '$' and tries again.
     * @throws IllegalArgumentException if all attempts fail
     */
    private fun String.scanClassName(): KClass<*> =
        classByName() ?:
            when (val dotRepl = reversed().replaceFirst('.', '$').reversed()) {
                this ->
                    throw IllegalArgumentException(this)
                else -> dotRepl.scanClassName()
            }

    private fun String.classByName(): KClass<*>? =
            try {
                Class.forName(this).kotlin
            } catch (e: ClassNotFoundException) {
                null
            }

}

data class SerialElement(
    val name: String,
    val annotations: List<Annotation>,
    val descriptor: SerialDescriptor,
    val optional: Boolean,
)
val SerialDescriptor.elements: List<SerialElement> get() =
    (0 until elementsCount).map {
        SerialElement(
            getElementName(it),
            getElementAnnotations(it),
            getElementDescriptor(it),
            isElementOptional(it),
        )
    }


fun SerialDescriptor.dump() = DefaultJson.indented.encodeToString(toJson())


fun SerialDescriptor.toJson(refs: MutableSet<String> = mutableSetOf()): JsonElement {
//    println("SerialDescriptor $serialName toJson, in refs ${serialName in refs}, refs $refs")
    return when {
        serialName in refs -> "ref: $serialName".toJsonElement()
        else -> {
            refs += serialName
            buildJsonObject {
                put("serialName", serialName)
                put("kind", kind.toString())
                put("nullable", isNullable)
                put("inline", isInline)
                put("annotations", annotations.mapToJsonArray { it.toString().toJsonElement() })
                put("elements", elements.mapToJsonArray { it.toJson(refs) })
            }
        }
    }
}

fun SerialElement.toJson(refs: MutableSet<String> = mutableSetOf()): JsonObject {
//    println("SerialElement $name toJson")
    return buildJsonObject {
        put("name", name)
        put("optional", optional)
        put("annotations", annotations.mapToJsonArray { it.toString().toJsonElement() })
        put("descriptor", descriptor.toJson(refs))
    }
}
