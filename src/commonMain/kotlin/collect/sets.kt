package org.jbali.collect


// --------------- immutable copies --------------- //

/**
 * @return [this] + [e]
 * @throws IllegalArgumentException if [this] already contains [e]
 */
fun <T> Set<T>.plusAbsent(e: T): Set<T> =
    this.plus(e)
        .also { if (it.size == this.size) throw IllegalArgumentException("Set $this already contains $e") }

/**
 * @return [this] + [other]
 * @throws IllegalArgumentException if this set already contains any element of [other]
 */
fun <T> Set<T>.plusAll(other: Set<T>): Set<T> {
    val result = this + other
    if (result.size != this.size + other.size) {
        throw IllegalArgumentException("Set $this already contains some elements of $other")
    }
    return result
}

/**
 * @return [this] - [e]
 * @throws NoSuchElementException if [this] does not contain [e]
 */
fun <T> Set<T>.minusPresent(e: T): Set<T> =
    this.minus(e)
        .also { if (it.size == this.size) throw NoSuchElementException("Set $this does not contain $e") }

/**
 * @return [this] - [other]
 * @throws NoSuchElementException if this set does not contain all elements of [other]
 */
fun <T> Set<T>.minusAll(other: Set<T>): Set<T> {
    val result = this - other
    if (result.size != this.size - other.size) {
        throw NoSuchElementException("Set $this does not contain all elements of $other")
    }
    return result
}


// ---------------- mutable ------------------ //

/**
 * Adds the specified element to this set, assuming it's not already present.
 *
 * This function is atomic and is safe to use concurrently if [MutableSet.add] is.
 * @throws IllegalArgumentException if the element is already present in the set
 */
fun <T> MutableSet<T>.mustAdd(e: T) {
    require(add(e)) { "Set already contains $e" }
}

/**
 * Adds all elements from the given [Iterable] to this set, assuming none of them are already present.
 *
 * This function is not atomic and assumes the set is not modified concurrently and the equality of elements is stable.
 * @throws IllegalArgumentException if any element in the [Iterable] is already present in this set, which remains unchanged.
 */
fun <T> MutableSet<T>.mustAddAll(s: Iterable<T>) {
    val tss = s.toSet()
    val intersection: Set<T> = intersect(tss)
    require(intersection.isEmpty()) { "Set already contains $intersection" }
    addAll(s)
}

/**
 * Removes the specified element from this set, assuming it's present.
 *
 * This function is atomic and is safe to use concurrently if [MutableSet.remove] is.
 * @throws NoSuchElementException if the element is not present in the set.
 */
fun <T> MutableSet<T>.mustRemove(e: T) {
    if (!remove(e)) {
        throw NoSuchElementException("Set does not contain $e")
    }
}

/**
 * Removes all elements from the given [Iterable] from this set, assuming all of them are present.
 *
 * This function is not atomic and assumes the set is not modified concurrently and the equality of elements is stable.
 * @throws NoSuchElementException if any element in the [Iterable] is not present in this set, which remains unchanged.
 */
fun <T> MutableSet<T>.mustRemoveAll(s: Iterable<T>) {
    val tss = s.toSet()
    val intersection = intersect(tss)
    require(intersection.size == s.count()) { "Set does not contain $intersection" }
    removeAll(tss)
}

/**
 * Adds all elements in [s] to this set.
 *
 * This function is not atomic and assumes the set is not modified concurrently and the equality of elements is stable.
 * @return The number of elements actually added to this set, i.e. that were not already in this set.
 */
fun <T> MutableSet<T>.countAddAll(s: Iterable<T>): Int {
    val oldSize = size
    addAll(s)
    return size - oldSize
}


// -------------- read-only ----------------- //

/**
 * @return Whether any set in this iterable intersects with (i.e. has common elements with) any other set in this iterable.
 */
fun <T> Iterable<Set<T>>.intersect(): Boolean {
    val seen = mutableSetOf<T>()
    for (set in this) {
        if (seen.countAddAll(set) < set.size) {
            return true
        }
    }
    return false
}
