package org.jbali.util

import org.jbali.util.OnceEvent.NotWaitingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.PreDestroy
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val mehLog = LoggerFactory.getLogger(Event::class.java)!!

typealias ListenerErrorCallback<P> = ((l: EventHandler<out P>, e: Throwable) -> Unit)

/**
 * Allows an event property to be declared like:
 * val onChange by EventDelegate<ChangeInfo>()
 */
// TODO make Event a delegate provider, enables:
// val onBla: Event<Foo> by Event
open class EventDelegate<P> : ReadOnlyProperty<Any?, Event<P>> {
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
class OnceEventDelegate<P> : ReadOnlyProperty<Any?, OnceEvent<P>> {
    private val constructed = AtomicReference<OnceEvent<P>>()
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): OnceEvent<P> {
        val c = constructed.get()
        return if (c != null) c else {
            // cannot use Lazy because this constructor requires the property
            val newc = OnceEvent<P>(thisRef, property)
            constructed.compareAndSet(null, newc)
            constructed.get()
        }
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
fun <P> Listenable<P>.smartListen(name: String, callback: (arg: P) -> SmartListenerResult): ListenerReference {

    val lock = Any()

    val sureListener: ListenerReference
    synchronized(lock) {
        var listener: ListenerReference? = null
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
            mehLog.warn("Error getting name for callback", e)
            "???"
        }, callback)
        listeners.add(l)
        return l
    }

    override fun listen(callback: (arg: P) -> Unit): EventListener<P> =
        listen(name, callback)

    // TODO why does this exist and doesn't the real dispatch have @JvmOverloads?
    fun dispatch(data: P) {
        dispatch(data, ListenerErrorCallbacks.default)
    }

    /**
     * Invoke all listeners in the current thread.
     * If a listener throws an exception, it is passed to errCb.
     * If that throws too, both exceptions are simply printed to stderr.
     *
     * An exception to the above is that any AssertionError is rethrown wrapped in an AssertionError with a message.
     * In that case, any remaining listeners will not be invoked.
     */
    open fun dispatch(data: P, errCb: ListenerErrorCallback<P> = ListenerErrorCallbacks.default) {
        for (l in listeners.toList()) {
            l.call(data, errCb)
        }
    }

    // TODO move to Listenable with @JvmDefault
    fun listenVoid(callback: Runnable) = listen { callback.run() }

    fun dispatch(data: P, errLog: Logger) {
        dispatch(data, ListenerErrorCallbacks.logging(errLog))
    }

    fun dispatch(data: P, throwAsAssert: Boolean) {
        dispatch(data, ListenerErrorCallbacks.create(fatalRethrow = throwAsAssert))
    }

    @JvmOverloads
    inline fun dispatch(
            noinline errCb: ListenerErrorCallback<P> = ListenerErrorCallbacks.default,
            lazyData: () -> P
    ) {
        if (hasListeners()) {
            dispatch(
                    data = lazyData(),
                    errCb = errCb
            )
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
 * An [Event] that can be dispatched only once.
 * Afterwards, all listeners will be automatically detached.
 * During and after this single dispatch, it will disallow adding new listeners by throwing [NotWaitingException].
 * However, it does offer the extra method [listenOrHandle] which can be used to run a callback exactly once.
 */
class OnceEvent<P>(
        name: String? = null
): Event<P>(name) {

//    companion object {
//        fun <P> provideDelegate(thisRef: Any?, property: KProperty<*>) =
//            OnceEventDelegate<P>()
//    }

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
    // Any? because P can neither be null nor lateinit, and don't want to use a Box just for this
    private var arg: Any? = null

    val state get() = lock.read { pState }

    override fun listen(name: String?, callback: (arg: P) -> Unit): EventListener<P> =
            listenImpl(name, callback, orHandle = false)!!

    /**
     * If this event has been (or is being) dispatched, immediately call [callback]
     * with the event data, and return `null`.
     *
     * Otherwise, attach it as listener, which is returned.
     *
     * If [callback] is invoked immediately, it is done so in the current thread, and any exceptions are not caught.
     */
    @JvmOverloads
    fun listenOrHandle(name: String? = null, callback: (arg: P) -> Unit): EventListener<P>? =
            listenImpl(name, callback, orHandle = true)

    private fun listenImpl(name: String?, callback: (arg: P) -> Unit, orHandle: Boolean): EventListener<P>? {

        var ret: EventListener<P>? = null
        lateinit var post: () -> Unit

        lock.read {
            if (pState != State.WAITING) {
                if (!orHandle) {
                    throw NotWaitingException("Cannot listen to $this, it's $pState")
                } else {
                    post = {
                        @Suppress("UNCHECKED_CAST")
                        callback(arg as P)
                    }
                    ret = null
                }
            } else {
                post = {}
                ret = super.listen(name, callback)
            }
        }

        post()

        return ret
    }

    // for JVM only
    fun listenOrHandleVoid(callback: Runnable) = listenOrHandle { callback.run() }

    override fun dispatch(data: P, errCb: ListenerErrorCallback<P>) {
        lock.write {
            if (pState != State.WAITING)
                throw IllegalStateException("Cannot dispatch $this, it's $pState")

            pState = State.IN_DISPATCH
            // TODO the IN_DISPATCH state is useless if the lock is not released here

            arg = data
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

interface EventHandler<P> {
    val name: String?
    val event: Event<P>?

    fun toStringImpl() =
        "${event}.listener($name)"

    companion object {
        fun <P> callWithErrorHandling(
            handler: EventHandler<P>,
            callback: (arg: P) -> Unit,
            data: P,
            errCb: ListenerErrorCallback<P> = ListenerErrorCallbacks.default
        ) {
            try {
                callback(data)
            } catch (ae: AssertionError) {
                throw EventAssertionError("In $this: $ae", ae)
            } catch (e: Throwable) {

                // shortcut!
                if (errCb == ListenerErrorCallbacks.fatalRethrowing) {
                    throw FatalEventError(e)
                }

                try {
                    errCb.invoke(handler, e)
                } catch (fee: FatalEventError) {
                    // fee will have e as cause and can be rethrown
                    throw fee
                } catch (cbE: Throwable) {
                    e.printStackTrace()
                    cbE.printStackTrace()
                }
            }
        }
    }
}

class EventAssertionError(m: String, c: Throwable) : AssertionError(m, c)

class EventListener<P>(
        private val eventRef: WeakReference<Event<P>>,
        override val name: String?,
        val callback: (arg: P) -> Unit
) : EventHandler<P>, ListenerReference {

    override val event: Event<P>?
        get() = eventRef.get()

    @JvmOverloads
    fun call(data: P, errCb: ListenerErrorCallback<P> = ListenerErrorCallbacks.default) {
        EventHandler.callWithErrorHandling(this, callback, data, errCb)
    }

    @PreDestroy
    override fun detach() {
        if (event?.listeners?.remove(this) == false) {
            mehLog.warn("")
        }
    }

    val attached get() =
        event?.listeners?.contains(this) ?: false

    override fun toString() = toStringImpl()

}

fun Iterable<ListenerReference>.detach() = this.forEach { it.detach() }

// TODO class ParameterlessEvent

// empty dispatch variant for Unit (void) events
fun Event<Unit>.dispatch(errCb: ListenerErrorCallback<Unit> = ListenerErrorCallbacks.default) {
    dispatch(Unit, errCb)
}
fun Event<Unit>.dispatch(errLog: Logger = ListenerErrorCallbacks.defaultErrorLog) {
    dispatch(Unit, ListenerErrorCallbacks.logging(errLog))
}
fun Event<Unit>.dispatch() {
    dispatch(Unit, ListenerErrorCallbacks.default)
}
fun EventListener<Unit>.call(errCb: ListenerErrorCallback<Unit> = ListenerErrorCallbacks.default) {
    call(Unit, errCb)
}

object ListenerErrorCallbacks {

    val defaultErrorLog: Logger = LoggerFactory.getLogger(ListenerErrorCallbacks::class.java)

    val default = createLogging(defaultErrorLog)

    fun logging(log: Logger): ListenerErrorCallback<*> =
        if (log == defaultErrorLog) {
            default
        } else {
            createLogging(log)
        }

    private fun createLogging(log: Logger): ListenerErrorCallback<*> =
        { l, e ->
            log.error("Error in $l", e)
        }

    /**
     * An event exception callback that rethrows the exception in an [AssertionError],
     * which will propagate up to the dispatcher.
     */
    val fatalRethrowing: ListenerErrorCallback<*> = { _: EventHandler<*>, e: Throwable ->
        throw FatalEventError(e)
    }

    fun create(fatalRethrow: Boolean = false): ListenerErrorCallback<*> =
        if (fatalRethrow) fatalRethrowing
        else default

}

class FatalEventError(e: Throwable) : AssertionError("FATAL $e", e)
