package org.jbali.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val log = LoggerFactory.getLogger(Event::class.java)!!

typealias ListenerErrorCallback<P> = ((l: EventListener<P>, e: Throwable) -> Unit)

fun loggingErrorCallback(event: Event<*>, log: Logger): ListenerErrorCallback<*> = { l, e ->
    log.error("Error in $event listener ${l.name}", e)
}

class EventDelegate<P> {
    private val constructed = AtomicReference<Event<P>>()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Event<P> {
        val c = constructed.get()
        return if (c != null) c else {
            val newc = Event<P>(property)
            constructed.compareAndSet(null, newc)
            constructed.get()
        }
    }
}

class Event<P>(
        val name: String? = null
){

    constructor(dispatcher: String, prop: KProperty<*>): this("$dispatcher.${prop.name}")
    constructor(dispatcher: KClass<*>, prop: KProperty<*>): this("${dispatcher.qualifiedName}.${prop.name}")
    constructor(prop: KProperty<*>): this(prop.toString().removePrefix("val ").removePrefix("var ").replaceAfter(':', "").dropLast(1))

    val listeners: MutableSet<EventListener<P>> = ConcurrentHashMap.newKeySet()

    fun listen(name: String?, callback: (arg: P) -> Unit): EventListener<P> {
        val l = EventListener(WeakReference(this), name ?: callback.toString(), callback)
        listeners.add(l)
        return l
    }
    fun listen(callback: (arg: P) -> Unit) = listen(null, callback)
    operator fun invoke(callback: (arg: P) -> Unit) = listen(null, callback)

    // for easier Java interop
    fun listenVoid(callback: Runnable) = listen { callback.run() }

    fun dispatch(data: P, errCb: ListenerErrorCallback<P>? = null) {
        for (l in listeners.toList()) {
            l.call(data, errCb)
        }
    }
    fun dispatch(data: P, errLog: Logger) {
        dispatch(data, loggingErrorCallback(this, errLog))
    }

    override fun toString() = "Event[$name]"

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
        event.get()?.listeners?.remove(this)
    }
    override fun toString() = "Event[$name]"
}

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

interface Observable<T> {
    val value: T
    val onChange: Event<T>

    operator fun invoke() = value

    fun bind(handler: (T) -> Unit) =
            onChange.listen(handler).call(value)

    fun bindj(handler: Consumer<T>) =
            bind { handler.accept(it) }

    fun <D> derived(derivation: (T) -> D): Observable<D> =
            DerivedObservable(this, derivation)

}

open class MutableObservable<T>(initialValue: T): Observable<T> {

    @Volatile
    override var value = initialValue
        set(n) {
            field = n
            onChange.dispatch(n)
        }

    override val onChange: Event<T> = Event(this::onChange)

    fun readOnly(): Observable<T> = this

}

open class DerivedObservable<I, O>(
        source: Observable<I>,
        derivation: (I) -> O
): MutableObservable<O>(derivation(source())) {

    init {
        source.onChange.listen {
            value = derivation(it)
        }
    }

}
