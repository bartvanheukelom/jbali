package org.jbali.events

import org.jbali.util.HasBetterKotlinAlternative
import org.jbali.util.boxed
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier
import javax.annotation.PreDestroy
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// Ovservables TODO I guess rename because of java.util.Observable (and put in own package, or with events)

interface Observable<out T>: Supplier<@UnsafeVariance T>, Function0<T>, Listenable<T>, ReadOnlyProperty<Any?, T> {

    /**
     * Event that is dispatched whenever the value changes, with the old and new value.
     * If you only need the new value, use [onNewValue] instead.
     */
    val onChange: Event<Change<@UnsafeVariance T>>

    /**
     * Convenience event whose data is only the new value. Is dispatched exactly once for each [onChange] event.
     * If you need the old value, use [onChange] instead.
     *
     * [Observable] implementors, see [setupNewValueDispatch].
     */
    val onNewValue: Event<@UnsafeVariance T>

    override fun listen(name: String?, callback: (arg: T) -> Unit) =
            onNewValue.listen(name, callback)

    override operator fun invoke() = get()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            get()

    /**
     * Register a handler that will be called immediately with the current value,
     * and whenever the value changes.
     */
    fun bind(handler: (T) -> Unit): ListenerReference =
            onNewValue.listen(handler).apply {
                EventHandler.callWithErrorHandling(
                    handler = object : EventHandler<T> {
                        override val name: String get() = "bind"
                        override val event: Event<@UnsafeVariance T> get() = onNewValue
                    },
                    callback = handler,
                    data = get()
                )
            }

    fun bindChange(handler: (before: T?, after: T) -> Unit): ListenerReference {
        handler(null, get())
        return onChange.listen {
            handler(it.before, it.after)
        }
    }

    /**
     * For Java only: Register a [Consumer] handler that will be called immediately with the current value,
     * and whenever the value changes.
     */
    @HasBetterKotlinAlternative("bind")
    fun bindj(handler: Consumer<@UnsafeVariance T>): ListenerReference =
            bind { handler.accept(it) }

    /**
     * [Observable] that is derived from this observable, called its source.
     * When the source is changed, the [derivation] function is executed with its new value once,
     * and the result is stored and distributed further.
     *
     * If [derivation] throws during the initial call, the exception is propagated.
     * If it throws when handling a source change, the exception is logged by [ListenerErrorCallbacks.default]
     * and the derived observable maintains its outdated value until the next source change.
     *
     * If [derivation] is trivial, consider using [sub] instead.
     */
    fun <D> derived(derivation: (T) -> D): Observable<D> =
            DerivedObservable(this, derivation)

    /**
     * Create an [Observable] that is derived from this observable, called its source.
     * When the source is changed, the [getter] function is executed on the values in the [Change],
     * and the result is distributed further, but not stored.
     *
     * Each call to [get] will get the current value from the source and return the result of applying [getter] to it.
     * If this is expensive or otherwise undesirable, consider using [derived] instead.
     */
    fun <D> sub(getter: (T) -> D): Observable<D> =
            ObservableSub(this, getter)
    
    companion object {
        /**
         * Create a static [Observable] (i.e. one that never changes) around [value].
         */
        fun <T> of(value: T): Observable<T> = MutableObservable(value)
    }

}


/**
 * Simple container for a [before] and [after] value of the same type, representing a change.
 *
 * [before] and [after] may be equal. [different] reflects whether they are actually different.
 */
data class Change<out T>(
    val before: T,
    val after: T,

    /**
     * If further events are dispatched as a direct result of this change,
     * and this is true, those events must be dispatched in non-exception swallowing mode.
     */
    val throwListenerExceptions: Boolean = false
) {
    
    /**
     * `before != after`
     */
    val different: Boolean = before != after
    
    /**
     * Return [after] if it's [different] from [before], else `null`.
     * Note that if [T] is nullable, `null` can also simply be the value of [after].
     * Use [boxedAfterIfDifferent] to distinguish between these cases.
     */
    val afterIfDifferent get() = if (different) after else null
    
    /**
     * Return [after] in a [org.jbali.util.Box] if it's [different] from [before], else `null`.
     * That is, if the value of [after] is `null`, this will return `Box(null)`.
     * If [T] is not nullable, consider using [afterIfDifferent] instead.
     */
    val boxedAfterIfDifferent get() = if (different) after.boxed() else null
    
}

/**
 * Create a [Change] from this value to [after].
 */
infix fun <T> T.changedTo(after: T): Change<T> =
        Change(before = this, after = after)


private fun <T> Observable<T>.setupNewValueDispatch() {
    // TODO onChange.forwardTo(onNewValue), which preserves the error callback
    onChange.listen {
        onNewValue.dispatch(
            data = it.after,
            throwAsAssert = it.throwListenerExceptions
        )
    }
}


/**
 * Container for a mutable reference of type [T] that also implements [Observable].
 * Changes to the reference are distributed to the listeners.
 */
sealed class MutableObservableBase<T>(initialValue: T, name: String? = null): Observable<T> {

    val ref = AtomicReference<T>(initialValue)

    /**
     * Setter: update the value, and if it is not equal to the current value,
     * will dispatch the onChange event with the new value.
     */
    var value: T
        get() = ref.get()
        set(n) { updateValue(n) }
    
    /**
     * Update the value, and only if it is not equal to the current value,
     * will dispatch the onChange event with the new value.
     *
     * If you don't use any other parameters than [n], you may also use the [value] setter instead.
     *
     * @param n The new value
     * @param throwAsAssert If true, will throw an [AssertionError] if any listener throws an exception. Useful for tests.
     */
    fun updateValue(n: T, throwAsAssert: Boolean = false) {
        val o = ref.getAndSet(n)
        if (n != o) {
            onChange.dispatch(Change(
                    before = o,
                    after = n,
                    throwListenerExceptions = throwAsAssert
            ), throwAsAssert)
        }
    }

    final override val onChange:   Event<Change<T>> = Event("${name ?: "MutableObservable"}.onChange"  )
    final override val onNewValue: Event<        T> = Event("${name ?: "MutableObservable"}.onNewValue")

    init {
        setupNewValueDispatch()
    }

    override fun get() = value
    
    /**
     * Returns a read-only view of this [MutableObservable].
     * That is, returns this same instance, but upcast to the read-only [Observable] interface.
     */
    fun readOnly(): Observable<T> = this

    @PreDestroy
    @MustBeInvokedByOverriders
    open fun destroy() {
        onChange.detachListeners()
        onNewValue.detachListeners()
    }

}


class MutableObservable<T>(initialValue: T, name: String? = null) :
    MutableObservableBase<T>(initialValue, name)



/**
 * Implementation of [Observable.sub], see doc there.
 */
private class ObservableSub<I, O>(
    val source: Observable<I>,
    val getter: (I) -> O
): Observable<O> {

    override val onChange: Event<Change<O>> = Event(this::onChange)
    override val onNewValue: Event<O> = Event(this::onNewValue)

    init {
        setupNewValueDispatch()
    }

    override fun get() = getter(source())

    private val sourceListener =
        source.onChange.listen {
            onChange.dispatch(errCb = ListenerErrorCallbacks.create(it.throwListenerExceptions)) {
                Change(getter(it.before), getter(it.after), it.throwListenerExceptions)
            }
        }

    @PreDestroy fun destroy() {
        sourceListener.detach()
        onChange.detachListeners()
        onNewValue.detachListeners()
    }

}



/**
 * Implementation of [Observable.derived], see doc there.
 */
private class DerivedObservable<I, O>(
    source: Observable<I>,
    derivation: (I) -> O
): MutableObservableBase<O>(derivation(source())) {

    private val listener = source.listen {
        value = derivation(it)
    }

    @PreDestroy
    override fun destroy() {
        listener.detach()
        super.destroy()
    }

}
