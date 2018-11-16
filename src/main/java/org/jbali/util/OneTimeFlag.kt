package org.jbali.util

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

/**
 * A flag that can go from unflagged (false) to flagged (true) exactly once, in an atomic way.
 * Can be used as a delegate for a Boolean property.
 */
class OneTimeFlag {
    private val a = AtomicReference<Instant>()

    operator fun invoke() = a.get() != null

    /**
     * @return Whether it was already flagged.
     */
    fun flagIfUnflagged() =
            a.compareAndSet(null, Instant.now())

    /**
     * @throws IllegalStateException if already flagged (with time in message).
     */
    fun flag() {
        lazy {  }
        if (!flagIfUnflagged()) throw IllegalStateException("Already flagged @ ${a.get()}")
    }

    // allow to be used as a Boolean property delegate
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = a.get() != null
    operator fun setValue(thisRef: Any?, property: KProperty<*>, v: Boolean) {
        if (!v) throw IllegalArgumentException("Can only set to true")
        else flag()
    }

}
