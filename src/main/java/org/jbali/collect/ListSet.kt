package org.jbali.collect

/**
 * Marker interface that indicates both List and Set semantics
 */
interface ListSet<T> : List<T>, Set<T> {
    override fun spliterator() = (this as List<T>).spliterator()
}

class ListSetImpl<T> private constructor(
        val items: List<T>,
        @Suppress("UNUSED_PARAMETER") overloadMarker: Unit
) : ListSet<T>, RandomAccess {

    init {
        TODO()
    }
    override val size: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun contains(element: T): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun get(index: Int): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun indexOf(element: T): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun lastIndexOf(element: T): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(): ListIterator<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(index: Int): ListIterator<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    constructor(items: Iterable<T>) : this(
            items
                    .ensureRandomAccessList()
                    .also { require(it.toSet().size != it.size) },
            Unit
    )
}

fun <T> Iterable<T>.ensureRandomAccessList(): List<T> = if (this is List && this is RandomAccess) this else this.toList()
fun <T> Iterable<T>.toListSet() = if (this is ListSet) this else ListSetImpl(this)
