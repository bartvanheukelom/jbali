package org.jbali

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * These tests document some details of Kotlin behaviour (and detects whether they change, but that's not expected).
 */
class KotlinTest {

    private interface FooBar<T> {
        fun foo(x: List<String>)
        fun foot(y: List<T>)
    }

    @Test fun kTypeClass() {



        val xp = FooBar<*>::foo.valueParameters.single { it.name == "x" }

        // List<String>
        assertNotEquals<Any>(List::class, xp.type)
        assertTrue(xp.type.classifier is KClass<*>)
        assertEquals(List::class, xp.type.classifier)

        // <String>
        val xpListString = xp.type.arguments.first()
        // String
        val xpListStringClass = xpListString.type!!.classifier
        assertTrue(xpListStringClass is KClass<*>, "$xpListStringClass")
        assertEquals(String::class, xpListStringClass)



        val yp = FooBar<*>::foot.valueParameters.single { it.name == "y" }

        // List<T>
        assertNotEquals<Any>(List::class, yp.type)
        assertTrue(yp.type.classifier is KClass<*>)
        assertEquals(List::class, yp.type.classifier)

        // <T>
        val ypListT = yp.type.arguments.first()
        // T
        val tpListTClass = ypListT.type!!.classifier
        assertFalse(tpListTClass is KClass<*>, "$tpListTClass")

    }

    @Test fun reflectionNullability() {
        for (iface in listOf(
                JavaTestInterface::class, KotlinTestInterface::class
        )) {
            println("============ $iface ============")

            for (nullable in listOf(false, true)) {
                val nullability = if (nullable) "nullable" else "notNull"
                val func = iface.functions.single {
                    it.name == nullability
                }

                println("----------------- ${func.name} --------------")

                func.valueParameters.forEachIndexed { i, p ->
                    println()
                    println("J: ${func.javaMethod!!.parameterTypes[i]} ${func.javaMethod!!.parameters[i].name}")
                    println("K: ${p.name}: ${p.type}")
                    println("Param:")
                    p.annotations.forEach {
                        println("   $it")
                    }
                    println("Type:")
                    p.type.annotations.forEach {
                        println("   $it")
                    }

                    assertEquals(nullable, p.type.isMarkedNullable, "$p nullable")
                }
            }
        }
    }

}

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class KotlinReflectNotExpectedToSeeNullable

interface KotlinTestInterface {

    /**
     * KType.isMarkedNullable should return false for these.
     */
    fun notNull(
            a: Int,
            // contradiction, who wins?
            @javax.annotation.Nullable b: Int,
            // jetbrains annot is erased
            @Nullable c: Int
    )

    /**
     * KType.isMarkedNullable should return true for these.
     */
    fun nullable(
            x: Int?,
            // jetbrains annot is erased
            @NotNull y: Int?
    )
}
