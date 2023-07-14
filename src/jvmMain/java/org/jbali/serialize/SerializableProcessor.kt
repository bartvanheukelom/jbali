package org.jbali.serialize

import org.jbali.util.logger
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.tools.Diagnostic

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.TYPE)
annotation class SuppressNotSerializable(
    val value: String = "",
)

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("*")
class SerializableProcessor : AbstractProcessor() {
    
    // TODO
    //      - instead of serializable bool, return yes/no/unknown
    //      - configurable strictness: no-unknown, allow-unknown, warn-only

    companion object {
        private val log = logger<SerializableProcessor>()
        private val skipModifiers = setOf(Modifier.STATIC, Modifier.TRANSIENT)
        private val collectionIfaces = setOf(
            "java.util.Collection",
            "java.util.List",
            "java.util.Set",
            "java.util.Map",
            "java.util.Map\$Entry",
        )
        private val collectionIfacesSimple = collectionIfaces.map { it
            .removePrefix("java.util.")
            .replace('$', '.') // inner classes
        }
    }
    
    init { log.info("init") }
    
    private val tSerializable: TypeMirror by lazy { // processingEnv is inited late
        log.info("Initializing Serializable type")
        processingEnv.elementUtils.getTypeElement("java.io.Serializable").asType()
    }
    private val tSuppressNotSerializable: TypeMirror by lazy {
        log.info("Initializing SuppressNotSerializable type")
        processingEnv.elementUtils.getTypeElement(SuppressNotSerializable::class.java.name).asType()
    }
    
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
//        log.info("Processing annotations $annotations in round $roundEnv")
        roundEnv.rootElements.asSequence()
            .filterIsInstance<TypeElement>()
            .filter { it.asType().implementsSerializable() }
            .forEach { checkSerializable(it) }
        return false // we don't claim any annotations
    }
    
    private var errorCount = 0
    
    private fun checkSerializable(element: TypeElement) {
//        log.info("Processing Serializable element $element")
        
        // check that all fields are Serializable too
        if (element.getAnnotation(SuppressNotSerializable::class.java) == null) {
            processingEnv.elementUtils.getAllMembers(element)
                .asSequence()
                .filter { it.kind.isField && it.modifiers.none(skipModifiers::contains) }
                .filter { it.getAnnotation(SuppressNotSerializable::class.java) == null }
//                .filter { !it.asType().isSerializable() }
                .forEach { member ->
                    try {
                        val fieldType = member.asType()
                        fieldType.isSerializable()?.let { error ->
                            val ec = ++errorCount
                            log.warn("Error #$ec element=$element member=$member")
                            processingEnv.messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "[$ec] '$fieldType': ${error.reversed().joinToString(" ")}",
                                member
                            )
                        }
                    } catch (e: Exception) {
                        log.error("error checking type of field $member", e)
                        processingEnv.messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "error checking field type: ${e.message}",
                            member
                        )
                    }
                }
        }
    }
    
    /**
     * Returns true if this type is Serializable, or false if it's not.
     *
     * As an example, `BaseType<@SuppressNotSerializable A, B, ...>` is considered Serializable if:
     *
     * - BaseType implements Serializable, or is exactly one of the collection interfaces
     * - (A is not checked)
     * - B and all other type args are Serializable according to these same rules
     */
    private fun TypeMirror.isSerializable(): List<String>? =
        when {
            getAnnotation(SuppressNotSerializable::class.java) != null -> null
            // the above doesn't work for type-use annotations, but this does:
            annotationMirrors.any { it.annotationType == tSuppressNotSerializable } -> null
            
            this is PrimitiveType -> null
            this is NullType -> null
            
            this is ArrayType -> componentType.isSerializable()?.plus("component type")
            this is DeclaredType -> isSerializable()
            
            this is TypeVariable -> if (upperBound == null) listOf("unknown upper bound") else upperBound.isSerializable()?.plus("upper bound")
            
            this is IntersectionType -> if (bounds.none { it.isSerializable() == null }) listOf("no bounds are Serializable") else null // TODO log why they aren't
            this is UnionType -> alternatives.firstOrNull { it.isSerializable() != null }?.let { listOf("alternative '$it' is not Serializable") }
            
            this is WildcardType -> listOf("is wildcard type") // unknown
            
            this is NoType -> listOf("is no type") // TODO what even is this?
            this is ExecutableType -> listOf("is an executable type") // TODO what even is this?
            
            
            else -> listOf("is an uncheckable type")
        }
    
    
    private fun DeclaredType.isSerializable(): List<String>? {
        
        val tu = processingEnv.typeUtils
        
        if (!implementsSerializable()
            && !collectionIfaces.contains(tu.erasure(this).toString())
        ) {
            return listOf("doesn't implement java.io.Serializable, nor is exactly one of ${collectionIfacesSimple.joinToString()}")
        }
        
        typeArguments
            .forEach { arg ->
                arg.isSerializable()?.let {
                    return it.plus("type argument $arg")
                }
            }
        
        return null
    }
    
    private fun TypeMirror.implementsSerializable() =
        processingEnv.typeUtils.isAssignable(this, tSerializable)
    
}
