package org.jbali.proxy

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.tools.Diagnostic
import kotlin.math.absoluteValue

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GenerateProxy

@SupportedAnnotationTypes("org.jbali.proxy.GenerateProxy")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ProxyGenerator : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isNotEmpty()) {

            val gpa = annotations.single()
            check(gpa.qualifiedName.contentEquals(GenerateProxy::class.qualifiedName))

            for (el in roundEnv.getElementsAnnotatedWith(annotations.first())) {
                if (el.kind != ElementKind.INTERFACE) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "$el is not an interface", el)
                } else {
                    val iface = el as TypeElement
                    val pack = iface.enclosingElement as PackageElement
                    val proxyName = "${iface.simpleName}Proxy"

                    val methods = el.enclosedElements.filter { it.kind == ElementKind.METHOD }.map { it as ExecutableElement }

                    val code = """
                        |package ${pack.qualifiedName};
                        |
                        |import java.lang.reflect.*;
                        |
                        |class $proxyName implements ${iface.simpleName} {
                        |
                        |${methods.joinToString(separator = "") { m ->
                        """
                        |   private static final Method M_${m.mangledName};"""
                        }}
                        |
                        |   static {
                        |       try {
                        |${methods.joinToString(separator = "") { m ->
                        """
                        |          M_${m.mangledName} = ${iface.simpleName}.class.getDeclaredMethod("${m.simpleName}", new Class<?>[]{${m.parameters.joinToString {
                            "${it.asType()}.class"
                        }}});"""
                        }}
                        |       } catch (NoSuchMethodException e) {
                        |           throw new AssertionError(e);
                        |       }
                        |   }
                        |
                        |   private InvocationHandler handler;
                        |
                        |   public $proxyName(InvocationHandler handler) {
                        |       this.handler = handler;
                        |   }
                        |
                        ${methods.joinToString(separator = "") { ex ->
                        """
                        |   @Override
                        |   public ${ex.returnType} ${ex.simpleName}(${ex.parameters.joinToString { 
                            "${it.asType()} ${it.simpleName}"
                        }}) {
                        |       Object[] args = null; // TODO
                        |       Object res;
                        |       try {
                        |           res = handler.invoke(this, M_${ex.mangledName}, args);
                        |       } catch (Throwable e) {
                        |           throw new RuntimeException("TODO: " + e, e);
                        |       }
                        |
                                ${if (ex.returnType.kind != TypeKind.VOID) { """
                        |       return (${ex.returnType}) res;
                                """ } else { """
                        |       if (res != null) throw new RuntimeException("void method returned null");
                                """}
                                } 
                        |   }
                        """
                        }}
                        |}
                        |
                    """.trimMargin()

                    println(code)

                    processingEnv.filer.createSourceFile("$pack.$proxyName").openWriter().use {
                        it.write(code)
                    }
                }


            }

        }
        return false
    }
}

val ExecutableElement.mangledName: String get() = "${simpleName}_${parameters.hashCode().absoluteValue}"
