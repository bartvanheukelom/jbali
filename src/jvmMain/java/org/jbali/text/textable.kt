package org.jbali.text

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

inline fun <reified T : Any> textable(data: Iterable<T>): Sequence<String> =
    textable(T::class, data)

inline fun <reified T : Any> textable(data: Sequence<T>): Sequence<String> =
    textable(T::class, data)

fun <T : Any> textable(clazz: KClass<T>, data: Iterable<T>): Sequence<String> =
    textable(clazz, data.asSequence())

fun <T : Any> textable(clazz: KClass<T>, data: Sequence<T>): Sequence<String> {
    
    // TODO dedup with ObjMap (and cache)
    val props: List<KProperty1<T, *>> =
        when {
            clazz.isData -> {
                clazz.primaryConstructor!!.parameters.map { par ->
                    clazz.declaredMemberProperties.first { it.name == par.name }
                }
            }
            else -> {
                clazz.declaredMemberProperties
                    .filter { it.visibility == KVisibility.PUBLIC }
            }
        }
    
    return textableWithProps(props, data)
    
}

fun <T : Any> textableWithProps(props: List<KProperty1<T, *>>, data: Iterable<T>): Sequence<String> =
    textableWithProps(props, data.asSequence())

fun <T : Any> textableWithProps(props: List<KProperty1<T, *>>, data: Sequence<T>): Sequence<String> {
    
    // TODO make Column class which encapsulates header name and value getter
    
    val numCols = props.size
    
    val cols = 0 until numCols
    val colHeaders = cols.map { c -> props[c].name }
    val rows = data.map { r ->
        cols.map { c -> props[c].get(r)?.toString() ?: "" }
    }
    
    return textableWithHeaders(colHeaders, rows)
    
}

@Deprecated(message = "use unambiguous textableWithCols", replaceWith = ReplaceWith("textableWithCols(cols, rows)"))
fun <R> textable(
    cols: List<Pair<String, (R) -> Any?>>,
    rows: Iterable<R>,
) = textableWithCols(cols, rows)

fun <R> textableWithCols(
    cols: List<Pair<String, (R) -> Any?>>,
    rows: Iterable<R>,
) =
    // TODO make this function accept sequence and use that
    textableWithHeaders(
        colHeaders = cols.map { it.first },
        rows = rows.map { r ->
            cols.map { c -> c.second(r)?.toString() ?: "<null>" }
        },
    )

// TODO look into existing dataframe libraries
@Deprecated(message = "use unambiguous textableWithHeaders", replaceWith = ReplaceWith("textableWithHeaders(colHeaders, rows)"))
fun textable(
    colHeaders: List<String>,
    rows: List<List<String>>,
): Sequence<String> =
    textableWithHeaders(colHeaders, rows)

fun textableWithHeaders(
    colHeaders: List<String>,
    rows: List<List<String>>,
): Sequence<String> =
    textableWithHeaders(colHeaders, rows.asSequence())

fun textableWithHeaders(
    colHeaders: List<String>,
    rows: Sequence<List<String>>,
): Sequence<String> {
    
    val numCols = colHeaders.size
    val cols = 0 until numCols
    
    // TODO allow value to be a smart formatter which can use column width as parameter
    
    val colSizes = cols.map { c ->
        maxOf(
            rows.maxOfOrNull { it[c].length } ?: 0,
            colHeaders[c].length,
        )
    }
    
    return sequence {
        
        // header
        yield(cols.joinToString(" | ") { c ->
            colHeaders[c].padEnd(colSizes[c])
        })
        yield("-".repeat(colSizes.sum() + (numCols - 1) * 3))
        
        // data
        for (r in rows) {
            yield(cols.joinToString(" | ") { c ->
                r[c].padEnd(colSizes[c])
            })
        }
        
    }
}

inline fun <reified T : Any> Iterable<T>.toTableString() =
    // TODO optimized builder with fixed length
    textable(this).joinToString("\n") + "\n"

inline fun <reified T : Any> Sequence<T>.toTableString() =
    // TODO optimized builder with fixed length
    textable(this).joinToString("\n") + "\n"

fun String.layout(
    maxWidth: Int = 100,
    collapseLines: Boolean = true,
    ellipsis: String = "…"
): String =
    if (collapseLines) {
        // TODO optimize for really long strings
        val collapsed = lineSequence().joinToString("⏎")
        when {
            collapsed.length <= maxWidth ->
                collapsed
            else ->
                collapsed.substring(0, maxWidth - ellipsis.length) + ellipsis
        }
    } else {
        TODO()
    }
