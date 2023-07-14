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
    val reason: String = ""
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
    }
    
    init { log.info("init") }
    
    private val tSerializable: TypeMirror by lazy { // processingEnv is inited late
        log.info("Initializing Serializable type")
        processingEnv.elementUtils.getTypeElement("java.io.Serializable").asType()
    }
    
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
//        log.info("Processing annotations $annotations in round $roundEnv")
        roundEnv.rootElements.asSequence()
            .filterIsInstance<TypeElement>()
            .filter { it.asType().implementsSerializable() }
            .forEach { checkSerializable(it) }
        return false // we don't claim any annotations
    }
    
    private fun checkSerializable(element: TypeElement) {
        log.info("Processing Serializable element $element")
        
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
                        if (!fieldType.isSerializable()) {
                            processingEnv.messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "field of non-Serializable type '$fieldType' in Serializable class",
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
    private fun TypeMirror.isSerializable(): Boolean {
        
        // BaseType<A, B, ...>
        
        val tu = processingEnv.typeUtils
        val unknown = false // for the future
        
        return when {
            kind.isPrimitive -> true
            this is ArrayType -> componentType.isSerializable()
            this is DeclaredType -> run {
                
                if (!implementsSerializable()
                    && !collectionIfaces.contains(tu.erasure(this).toString())
                ) return@run false
                
                if (typeArguments.any {
                    it.getAnnotation(SuppressNotSerializable::class.java) == null &&
                        !it.isSerializable()
                }) return@run false
                
                return@run true
            }
            this is TypeVariable -> upperBound?.isSerializable() ?: unknown
            this is IntersectionType -> bounds.any { it.isSerializable() }
            this is UnionType -> alternatives.all { it.isSerializable() }
            this is WildcardType -> unknown
            else -> error("Unsupported kind $kind of type $this")
        }
        
    }
    
    private fun TypeMirror.implementsSerializable() =
        processingEnv.typeUtils.isAssignable(this, tSerializable)
    
}
