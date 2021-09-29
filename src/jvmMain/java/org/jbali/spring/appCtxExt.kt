package org.jbali.spring

import org.jbali.text.layout
import org.jbali.text.toTableString
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import java.util.function.Supplier

inline fun <reified T : Any> GenericApplicationContext.registerBean(
    vararg constructorArgs: Any
) {
    registerBean(T::class.java, *constructorArgs)
}

inline fun <reified T : Any> GenericApplicationContext.registerBean(
    crossinline supplier: () -> T
) {
    registerBean(T::class.java, Supplier { supplier() })
}

data class LoggedBean(
    val name: String,
    val type: String,
    val toString: String?,
)

fun ApplicationContext.allSingletons() =
    getBeansOfType<Any>(
        includeNonSingletons = true,
        allowEagerInit = false,
    )

fun ApplicationContext.singletonBeanTable(): String {
    return allSingletons().entries
            .sortedBy { (n, _) -> n }
            .map { (n, b) ->
                LoggedBean(
                    name = n,
                    type = b.javaClass.simpleName,
                    toString =
                    @Suppress("UNNECESSARY_SAFE_CALL") // some weirdo beans actually return null for toString()
                    b.toString()?.layout(maxWidth = 128),
                )
            }
            .toTableString()
}
