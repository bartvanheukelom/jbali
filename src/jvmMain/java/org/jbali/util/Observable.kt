package org.jbali.util

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier
import javax.annotation.PreDestroy
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// Ovservables TODO I guess rename because of java.util.Observable (and put in own package, or with events)

interface Observable<T>: Supplier<T>, Function0<T>, Listenable<T>, ReadOnlyProperty<Any?, T> {

    val onChange: Event<Change<T>>

    /**
     * Convenience event whose data is only the new value. Is dispatched exactly once for each [onChange] event.
     *
     * [Observable] implementors, see [setupNewValueDispatch].
     */
    val onNewValue: Event<T>

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
                        override val event: Event<T> get() = onNewValue
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
     * Register a handler that will be called immediately with the current value,
     * and whenever the value changes.
     *
     * For use by Java code only.
     */
    fun bindj(handler: Consumer<T>): ListenerReference =
            bind { handler.accept(it) }

    /**
     * [Observable] that is derived from this observable, called its source.
     * When the source is changed, the [derivation] function is executed with its new value once,
     * and the result is stored and distributed further.
     */
    fun <D> derived(derivation: (T) -> D): Observable<D> =
            DerivedObservable(this, derivation)

    /**
     * Create an [Observable] that is derived from this observable, called its source.
     * When the source is changed, the [getter] function is executed on the values in the [Change],
     * and the result is distributed further, but not stored.
     *
     * Calls to [get] will get the current value from the source and return the result of applying [getter] to it.
     */
    fun <D> sub(getter: (T) -> D): Observable<D> =
            ObservableSub(this, getter)

}

data class Change<T>(
        val before: T,
        val after: T,

        /**
         * If further events are dispatched as a direct result of this change,
         * and this is true, those events must be dispatched in non-exception swallowing mode.
         */
        val throwListenerExceptions: Boolean = false
) {
    val different: Boolean = before != after
    val afterIfDifferent get() = if (different) after else null
}

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

    final override val onChange: Event<Change<T>> = Event("${name ?: "MutableObservable"}.onChange")
    final override val onNewValue: Event<T> = Event("${name ?: "MutableObservable"}.onNewValue")

    init {
        setupNewValueDispatch()
    }

    override fun get() = value

    fun readOnly(): Observable<T> = this

    @PreDestroy
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
