package org.jbali.util

import org.jbali.streams.simpleReduce
import java.io.File
import kotlin.streams.toList

object Handled

data class RelativePath(val elements: List<String>) : Iterable<String> {

    private val string = elements.joinToString("/")

    val first get() =  elements.first()
    val last get() = elements.last()
    val empty get() = elements.isEmpty()

    val optString get() = if (empty) null else string

    constructor(elements: Iterable<String>): this(elements.toList())

    constructor(vararg elements: String): this(elements.toList().stream().flatMap {
        it.split("/".toRegex())
                .stream()
                .filter { it.isNotBlank() }
    }.toList())

    fun startsWith(vararg pre: String) = startsWith(RelativePath(*pre))
    fun startsWith(pre: RelativePath) =
            elements.size >= pre.elements.size
                    && elements.subList(0, pre.elements.size) == pre.elements

    fun subIfStartsWith(vararg pre: String) = subIfStartsWith(RelativePath(*pre))
    fun subIfStartsWith(pre: RelativePath) =  if (startsWith(pre)) subPath(pre.elements.size) else null

    inline fun dispatch(pre: String, then: (RelativePath) -> Handled) = dispatch(RelativePath(pre), then)
    inline fun dispatch(vararg pre: String, then: (RelativePath) -> Handled) = dispatch(RelativePath(*pre), then)
    inline fun dispatch(pre: RelativePath, then: (RelativePath) -> Handled) =
            if (!startsWith(pre)) false else {
                then(subPath(pre.elements.size))
                true
            }

    fun subPath(depth: Int) = RelativePath(elements.subList(depth, elements.size))

    fun asFileInDir(baseDir: File) =
            elements.stream().simpleReduce(baseDir, { f, p -> File(f, p) })

    fun equalTo(other: String) = equals(RelativePath(other))
    fun equalTo(vararg other: String) = equals(RelativePath(*other))
    override fun toString() = string
    operator fun get(i: Int) = elements[i]
    override fun iterator() = elements.iterator()
    operator fun plus(other: RelativePath) = RelativePath(elements + other.elements)

    fun dotNormalized(): RelativePath {
        val res = elements.toMutableList()
        var i = 0
        while (i < res.size) {
            val p = res[i]
            if (p.isEmpty() || p == ".") {
                res.removeAt(i)
                i--
            } else if (p == "..") {
                if (i == 0) throw IllegalArgumentException("Trying to .. too high")
                else {
                    res.subList(i - 1, i + 1).clear()
                    i -= 2
                }
            }
            i++
        }
        return RelativePath(res)
    }
}