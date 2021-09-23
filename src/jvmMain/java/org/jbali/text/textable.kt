package org.jbali.text

import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> textable(data: Iterable<T>): Sequence<String> {
    
    val clazz = T::class
    
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
    
    // TODO make Column class which encapsulates header name and value getter
    
    val numCols = props.size
    
    val cols = 0 until numCols
    val colHeaders = cols.map { c -> props[c].name }
    val rows = data.map { r ->
        cols.map { c -> props[c].get(r)?.toString() ?: "" }
    }
    
    return textable(colHeaders, rows)
    
}

// TODO look into existing dataframe libraries
fun textable(
    colHeaders: List<String>,
    rows: List<List<String>>,
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
