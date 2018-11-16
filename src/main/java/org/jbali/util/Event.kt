package org.jbali.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function.Supplier
import javax.annotation.PreDestroy
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val log = LoggerFactory.getLogger(Event::class.java)!!

typealias ListenerErrorCallback<P> = ((l: EventListener<P>, e: Throwable) -> Unit)

fun loggingErrorCallback(event: Event<*>, log: Logger): ListenerErrorCallback<*> = { l, e ->
    log.error("Error in $event listener ${l.name}", e)
}

/**
 * Allows an event property to be declared like:
 * val onChange by EventDelegate<ChangeInfo>()
 */
class EventDelegate<P> {
    private val constructed = AtomicReference<Event<P>>()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Event<P> {
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
    fun listen(callback: (arg: P) -> Unit) = listen(null, callback)
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

    override fun listen(name: String?, callback: (arg: P) -> Unit): EventListener<P> {
        val l = EventListener(WeakReference(this), name ?: callback.toString(), callback)
        listeners.add(l)
        return l
    }

    open fun dispatch(data: P, errCb: ListenerErrorCallback<P>? = null) {
        for (l in listeners.toList()) {
            l.call(data, errCb)
        }
    }

    // TODO move to Listenable with @JvmDefault
    fun listenVoid(callback: Runnable) = listen { callback.run() }

    fun dispatch(data: P, errLog: Logger) {
        dispatch(data, loggingErrorCallback(this, errLog))
    }

    override fun toString() = "Event[$name]"

}

/**
 * An Event that can be dispatched only once.
 * Afterwards, all listeners will be automatically detached.
 * During and after this single dispatch, it will disallow adding new listeners.
 */
class OnceEvent<P>(
        name: String? = null
): Event<P>(name) {

    constructor(dispatcher: Any?, prop: KProperty<*>): this(cname(dispatcher, prop))
    constructor(dispatcher: KClass<*>, prop: KProperty<*>): this(cname(dispatcher, prop))
    constructor(prop: KProperty<*>): this(cname(prop))

    val lock = ReentrantLock()
    private var dispatchState: String? = null

    override fun listen(name: String?, callback: (arg: P) -> Unit) =
            lock.withLock {
                if (dispatchState != null)
                    throw IllegalStateException("Cannot listen to $this, it $dispatchState")
                super.listen(name, callback)
            }

    override fun dispatch(data: P, errCb: ListenerErrorCallback<P>?) {
        lock.withLock {
            if (dispatchState != null)
                throw IllegalStateException("Cannot dispatch $this, it $dispatchState")
            dispatchState = "is being dispatched right now"
            super.dispatch(data, errCb)
            dispatchState = "was already dispatched at ${Instant.now()}"
            listeners.forEach { it.detach() }
        }
    }

    override fun toString() = "OnceEvent[$name]"

}

class EventListener<P>(
        val event: WeakReference<Event<P>>,
        val name: String?,
        val callback: (arg: P) -> Unit
) {

    private val defaultErrCb: ListenerErrorCallback<P> =
            loggingErrorCallback(event.get()!!, log)

    @JvmOverloads
    fun call(data: P, errCb: ListenerErrorCallback<P>? = null) {
        try {
            callback(data)
        } catch (e: Throwable) {
            try {
                (errCb ?: defaultErrCb).invoke(this, e)
            } catch (ee: Throwable) {
                ee.printStackTrace()
            }
        }
    }

    fun detach() {
        if (event.get()?.listeners?.remove(this) == false) {
            log.warn("")
        }
    }

    val attached get() =
        event.get()?.listeners?.contains(this) ?: false

    override fun toString() = "Event[$name]"
}

fun Iterable<EventListener<*>>.detach() = this.forEach { it.detach() }

// TODO class ParameterlessEvent

// empty dispatch variant for Unit (void) events
fun Event<Unit>.dispatch(errCb: ListenerErrorCallback<Unit>? = null) {
    dispatch(Unit, errCb)
}
fun Event<Unit>.dispatch(errLog: Logger = log) {
    dispatch(Unit, loggingErrorCallback(this, errLog))
}
fun EventListener<Unit>.call(errCb: ListenerErrorCallback<Unit>? = null) {
    call(Unit, errCb)
}

// Ovservables TODO move to own file and I guess rename because of java.util.Observable

interface Observable<T>: Supplier<T>, Function0<T>, Listenable<T> {
    val onChange: Event<T>

    override fun listen(name: String?, callback: (arg: T) -> Unit) =
            onChange.listen(name, callback)

    override operator fun invoke() = get()

    fun bind(handler: (T) -> Unit) =
            onChange.listen(handler).call(get())

    fun bindj(handler: Consumer<T>) =
            bind { handler.accept(it) }

    fun <D> derived(derivation: (T) -> D): Observable<D> =
            DerivedObservable(this, derivation)

    fun <D> sub(getter: (T) -> D): Observable<D> =
            ObservableSub(this, getter)

}

open class MutableObservable<T>(initialValue: T): Observable<T> {

    @Volatile
    var value = initialValue
        set(n) {
            if (field != n) {
                field = n
                onChange.dispatch(n)
            }
        }

    @Suppress("LeakingThis")
    override val onChange: Event<T> = Event(this::onChange)

    override fun get() = value
    fun readOnly(): Observable<T> = this

}

open class ObservableSub<I, O>(
        val source: Observable<I>,
        val getter: (I) -> O
): Observable<O> {

    @Suppress("LeakingThis")
    override val onChange: Event<O> = Event(this::onChange)

    override fun get() = getter(source())

    val listener = source.listen {
        onChange.dispatch(getter(it))
    }

    @PreDestroy fun destroy() {
        listener.detach()
    }

}

open class DerivedObservable<I, O>(
        source: Observable<I>,
        derivation: (I) -> O
): MutableObservable<O>(derivation(source())) {

    val listener = source.listen {
        value = derivation(it)
    }

    @PreDestroy fun destroy() {
        listener.detach()
    }

}
