@file:OptIn(ExperimentalSerializationApi::class)
package org.jbali.kotser.jsonSchema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.serializer
import org.jbali.kotser.jsonString
import org.jbali.reflect.isObject
import org.jbali.reflect.kClass
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

@OptIn(ExperimentalStdlibApi::class)
class JsonSchemaGenerator {

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

    inline fun <reified T> PrintWriter.generate() {
        generate(T::class)
    }

    fun PrintWriter.generate(
        type: KClass<*>,
//        strat:
    ) {
        queue(type)
        processQueue()
    }

    private val nothing: Nothing get() = throw AssertionError()

    private var inNamespace: String = ""
    
    private fun PrintWriter.closeNs() {
        if (inNamespace != "") {
            println("}\n\n")
        }
        inNamespace = ""
    }

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
    
            if (inNamespace != ns) {
                closeNs()
                println("export namespace $ns {")
                inNamespace = ns
            }
    
            when {
                clazz.isValue -> {
                    val typeDef = desc.typeName()
                    println("\n  export type $rn = $typeDef")
                }
                clazz.isSubclassOf(Enum::class) -> {
                    val typeDef = clazz.java.enumConstants!!.joinToString(" | ") {
                        jsonString((it as Enum<*>).name).toString() // TODO SerialName
                    }
                    println("\n  export type $rn = $typeDef")
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
        
                        var tn = eDesc.typeName()
                        if (tn.startsWith(inNamespace + ".")) {
                            tn = tn.removePrefix(inNamespace + ".")
                        }
        
                        println("    $eName${if (eOpt) "?" else ""}: $tn;")
                    }
    
                    println("  }")
    
                }
            }
            
        }
    
        closeNs()
        
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
                "kotlin.UByte"
                    -> "number"

                "kotlin.String" -> "string"
                "kotlin.Boolean" -> "boolean"



                "kotlinx.serialization.json.JsonElement" -> "string | number | boolean | null"
                "kotlinx.serialization.json.JsonObject" -> "{ [key: string]: (string | number | boolean | null) }"
                "kotlinx.serialization.json.JsonArray" -> "(string | number | boolean | null)[]"
                "kotlinx.serialization.json.JsonPrimitive" -> "string | number | boolean"

                "kotlin.collections.LinkedHashSet",
                "kotlin.collections.ArrayList" -> {
                    "${getElementDescriptor(0).typeName()}[]"
                }

                "kotlin.collections.LinkedHashMap" -> {
                    "{ [key: ${getElementDescriptor(0).typeName()}]: ${getElementDescriptor(1).typeName()} }"
                }

                else -> {

                    when {

                        serialName in phNames -> {
                            phReplaceNames!![phNames.indexOf(serialName)]
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
