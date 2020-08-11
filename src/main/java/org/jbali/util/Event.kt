package org.jbali.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import java.util.function.Supplier
import javax.annotation.PreDestroy
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val log = LoggerFactory.getLogger(Event::class.java)!!

typealias ListenerErrorCallback<P> = ((l: EventListener<out P>, e: Throwable) -> Unit)

/**
 * Allows an event property to be declared like:
 * val onChange by EventDelegate<ChangeInfo>()
 */
// TODO make Event a delegate provider, enables:
// val onBla: Event<Foo> by Event
class EventDelegate<P> : ReadOnlyProperty<Any?, Event<P>> {
    private val constructed = AtomicReference<Event<P>>()
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Event<P> {
        val c = constructed.get()
        return if (c != null) c else {
            // cannot use Lazy because this constructor requires the property
            val newc = Event<P>(thisRef, property)
            constructed.compareAndSet(null, newc)
            constructed.get()
        }
    }
}

/**
 * Allows an event property to be declared like:
 * val onChange by OnceEventDelegate<ChangeInfo>()
 */
class OnceEventDelegate<P> {
    private val constructed = AtomicReference<OnceEvent<P>>()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): OnceEvent<P> {
        val c = constructed.get()
        return if (c != null) c else {
            // cannot use Lazy because this constructor requires the property
            val newc = OnceEvent<P>(thisRef, property)
            constructed.compareAndSet(null, newc)
            constructed.get()
        }
    }
}

interface Listenable<P> {
    fun listen(name: String?, callback: (arg: P) -> Unit): EventListener<P>
    // TODO @JvmDefault but require JVM 1.8
    fun listen(callback: (arg: P) -> Unit) = listen(null, callback)

    class FatalEventError(e: Throwable) : AssertionError("FATAL $e", e)

    companion object {

        fun loggingErrorCallback(log: Logger): ListenerErrorCallback<*> = { l, e ->
            log.error("Error in $l", e)
        }

        val defaultLogErrorCallback = loggingErrorCallback(log)

        val rethrowEventErrorAsAssert: ListenerErrorCallback<*> = { _: EventListener<*>, e: Throwable ->
            throw FatalEventError(e)
        }

        fun errorCallback(throwAsAssert: Boolean = false) =
                if (throwAsAssert) rethrowEventErrorAsAssert
                else defaultLogErrorCallback

    }
}

sealed class SmartListenerResult
object KeepListening : SmartListenerResult()
object StopListening : SmartListenerResult()
class StopListeningDespiteException(val e: Throwable) : SmartListenerResult()
@Suppress("FunctionName")
fun SmartListenerResult(keepListening: Boolean) =
        if (keepListening) KeepListening
        else StopListening

/**
 * Listen to this event with a callback that can choose to detach itself by returning StopListening.
 */
fun <P> Listenable<P>.smartListen(name: String, callback: (arg: P) -> SmartListenerResult): EventListener<P> {

    val lock = Any()

    val sureListener: EventListener<P>
    synchronized(lock) {
        var listener: EventListener<P>? = null
        sureListener = listen(name) {
            synchronized(lock) {
                if (listener != null) {
                    val res = callback(it)
                    if (res is StopListeningDespiteException || res == StopListening) {
                        listener!!.detach()
                        listener = null
                    }
                    if (res is StopListeningDespiteException) {
                        throw res.e
                    }
                }
            }
        }
        listener = sureListener
    }

    return sureListener
}

/**
 * Listen to this event once, even if that listener throws an exception.
 */
fun <P> Listenable<P>.listenOnce(name: String, callback: (arg: P) -> Unit) =
        smartListen(name) {
            try {
                callback(it)
                StopListening
            } catch (e: Throwable) {
                StopListeningDespiteException(e)
            }
        }

open class Event<P>(
        val name: String? = null
): Listenable<P> {

    companion object {
        @JvmStatic protected fun cname(dispatcher: Any?, prop: KProperty<*>) = "[$dispatcher].${prop.name}"
        @JvmStatic protected fun cname(dispatcher: KClass<*>, prop: KProperty<*>) = "${dispatcher.qualifiedName}.${prop.name}"
        @JvmStatic protected fun cname(prop: KProperty<*>) =  prop.toString().removePrefix("val ").removePrefix("var ").replaceAfter(':', "").dropLast(1)
    }

    constructor(dispatcher: Any?, prop: KProperty<*>): this(cname(dispatcher, prop))
    constructor(dispatcher: KClass<*>, prop: KProperty<*>): this(cname(dispatcher, prop))
    constructor(prop: KProperty<*>): this(cname(prop))

    internal val listeners: MutableSet<EventListener<P>> = ConcurrentHashMap.newKeySet()
    fun hasListeners() = listeners.isNotEmpty()

    override fun listen(name: String?, callback: (arg: P) -> Unit): EventListener<P> {
        val l = EventListener(WeakReference(this), name ?: try {
            callback.toString()
        } catch (e: Throwable) {
            log.warn("Error getting name for callback", e)
            "???"
        }, callback)
        listeners.add(l)
        return l
    }

    // TODO why does this exist and doesn't the real dispatch have @JvmOverloads?
    fun dispatch(data: P) {
        dispatch(data, Listenable.defaultLogErrorCallback)
    }

    /**
     * Invoke all listeners in the current thread.
     * If a listener throws an exception, it is passed to errCb.
     * If that throws too, both exceptions are simply printed to stderr.
     *
     * An exception to the above is that any AssertionError is rethrown wrapped in an AssertionError with a message.
     * In that case, any remaining listeners will not be invoked.
     */
    open fun dispatch(data: P, errCb: ListenerErrorCallback<P> = Listenable.defaultLogErrorCallback) {
        for (l in listeners.toList()) {
            l.call(data, errCb)
        }
    }

    // TODO move to Listenable with @JvmDefault
    fun listenVoid(callback: Runnable) = listen { callback.run() }

    fun dispatch(data: P, errLog: Logger) {
        dispatch(data, Listenable.loggingErrorCallback(errLog))
    }

    fun dispatch(data: P, throwAsAssert: Boolean) {
        dispatch(data, Listenable.errorCallback(throwAsAssert))
    }

    @JvmOverloads
    inline fun dispatch(noinline errCb: ListenerErrorCallback<P> = Listenable.defaultLogErrorCallback, lazyData: () -> P) {
        if (hasListeners()) {
            dispatch(lazyData(), errCb)
        }
    }

    @PreDestroy
    fun detachListeners() {
        listeners.forEach { it.detach() }
    }

    // TODO function to detach all and prevent new listeners from being added

    override fun toString() = "Event[$name]"

}

/**
 * An Event that can be dispatched only once.
 * Afterwards, all listeners will be automatically detached.
 * During and after this single dispatch, it will disallow adding new listeners by throwing NotWaitingException.
 */
class OnceEvent<P>(
        name: String? = null
): Event<P>(name) {

    class NotWaitingException(m: String) : IllegalStateException(m)

    enum class State {
        WAITING,
        IN_DISPATCH,
        DONE
    }

    constructor(dispatcher: Any?, prop: KProperty<*>): this(cname(dispatcher, prop))
    constructor(dispatcher: KClass<*>, prop: KProperty<*>): this(cname(dispatcher, prop))
    constructor(prop: KProperty<*>): this(cname(prop))

    // mutable state with their lock
    private val lock = ReentrantReadWriteLock()
    private var pState = State.WAITING

    val state get() = lock.read { pState }

    override fun listen(name: String?, callback: (arg: P) -> Unit) =
            lock.read {
                if (pState != State.WAITING)
                    throw NotWaitingException("Cannot listen to $this, it's $pState")
                super.listen(name, callback)
            }

    override fun dispatch(data: P, errCb: ListenerErrorCallback<P>) {
        lock.write {
            if (pState != State.WAITING)
                throw IllegalStateException("Cannot dispatch $this, it's $pState")

            pState = State.IN_DISPATCH
            try { // no exceptions should be thrown, but eh
                super.dispatch(data, errCb)
                detachListeners()
            } finally {
                pState = State.DONE
            }

        }
    }

    override fun toString() = "OnceEvent[$name]"

}

class EventListener<P>(
        val event: WeakReference<Event<P>>,
        val name: String?,
        val callback: (arg: P) -> Unit
) {

    class EventAssertionError(m: String, c: Throwable) : AssertionError(m, c)

    @JvmOverloads
    fun call(data: P, errCb: ListenerErrorCallback<P> = Listenable.defaultLogErrorCallback) {
        try {
            callback(data)
        } catch (ae: AssertionError) {
            throw EventAssertionError("In $this: $ae", ae)
        } catch (e: Throwable) {

            // shortcut!
            if (errCb == Listenable.rethrowEventErrorAsAssert) {
                throw Listenable.FatalEventError(e)
            }

            try {
                errCb.invoke(this, e)
            } catch (fee: Listenable.FatalEventError) {
                // fee will have e as cause and can be rethrown
                throw fee
            } catch (cbE: Throwable) {
                e.printStackTrace()
                cbE.printStackTrace()
            }
        }
    }

    @PreDestroy
    fun detach() {
        if (event.get()?.listeners?.remove(this) == false) {
            log.warn("")
        }
    }

    val attached get() =
        event.get()?.listeners?.contains(this) ?: false

    override fun toString() = "${event.get()}.listener($name)"
}

fun Iterable<EventListener<*>>.detach() = this.forEach { it.detach() }

// TODO class ParameterlessEvent

// empty dispatch variant for Unit (void) events
fun Event<Unit>.dispatch(errCb: ListenerErrorCallback<Unit> = Listenable.defaultLogErrorCallback) {
    dispatch(Unit, errCb)
}
fun Event<Unit>.dispatch(errLog: Logger = log) {
    dispatch(Unit, if (errLog == log) Listenable.defaultLogErrorCallback else Listenable.loggingErrorCallback(errLog))
}
fun Event<Unit>.dispatch() {
    dispatch(log)
}
fun EventListener<Unit>.call(errCb: ListenerErrorCallback<Unit> = Listenable.defaultLogErrorCallback) {
    call(Unit, errCb)
}

// Ovservables TODO move to own file and I guess rename because of java.util.Observable

interface Observable<T>: Supplier<T>, Function0<T>, Listenable<T>, ReadOnlyProperty<Any?, T> {
    val onChange: Event<Change<T>>
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
    fun bind(handler: (T) -> Unit): EventListener<T> =
            onNewValue.listen(handler).apply { call(get()) }

    fun bindChange(handler: (before: T?, after: T) -> Unit): EventListener<Change<T>> {
        handler(null, get())
        return onChange.listen {
            handler(it.before, it.after)
        }
    }

    fun bindj(handler: Consumer<T>): EventListener<T> =
            bind { handler.accept(it) }

    /**
     * Create a derived observable that applies the derivation function to the source value,
     * and caches the result.
     */
    fun <D> derived(derivation: (T) -> D): Observable<D> =
            DerivedObservable(this, derivation)

    /**
     * Create a derived observable that applies the getter function to the source value
     * every time it is accessed (so it should be cheap).
     */
    fun <D> sub(getter: (T) -> D): Observable<D> =
            ObservableSub(this, getter)

}

data class Change<T>(val before: T, val after: T)

open class MutableObservable<T>(initialValue: T, name: String? = null): Observable<T> {

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
            onChange.dispatch(Change(o, n), throwAsAssert)
        }
    }

    final override val onChange: Event<Change<T>> = Event("${name ?: "MutableObservable"}.onChange")
    final override val onNewValue: Event<T> = Event("${name ?: "MutableObservable"}.onNewValue")

    init {
        onChange.listen { onNewValue.dispatch(it.after) }
    }

    override fun get() = value
    fun readOnly(): Observable<T> = this

    @PreDestroy
    open fun destroy() {
        onChange.detachListeners()
        onNewValue.detachListeners()
    }

}

class ObservableSub<I, O>(
        val source: Observable<I>,
        val getter: (I) -> O
): Observable<O> {

    override val onChange: Event<Change<O>> = Event(this::onChange)
    override val onNewValue: Event<O> = Event(this::onNewValue)

    override fun get() = getter(source())

    val listeners =
            listOf(
                    source.onChange.listen {
                        onChange.dispatch {
                            Change(getter(it.before), getter(it.after))
                        }
                    },
                    source.onNewValue.listen {
                        onNewValue.dispatch {
                            getter(it)
                        }
                    }
            )

    @PreDestroy fun destroy() {
        listeners.detach()
        onChange.detachListeners()
        onNewValue.detachListeners()
    }

}

open class DerivedObservable<I, O>(
        source: Observable<I>,
        derivation: (I) -> O
): MutableObservable<O>(derivation(source())) {

    val listener = source.listen {
        value = derivation(it)
    }

    @PreDestroy
    override fun destroy() {
        listener.detach()
        super.destroy()
    }

}
