package org.jbali.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val log = LoggerFactory.getLogger(Event::class.java)!!

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

    fun dispatch(data: P, errCb: ((l: EventListener<P>, e: Throwable) -> Unit)?) {
        for (l in listeners.toList()) {
            try {
                l.callback(data)
            } catch (e: Throwable) {
                try {
                    errCb?.invoke(l, e)
                } catch (ee: Throwable) {
                    ee.printStackTrace()
                }
            }
        }
    }
    fun dispatch(data: P, errLog: Logger = log) {
        dispatch(data, { l, e -> errLog.error("Error in $this listener ${l.name}", e) })
    }

    override fun toString() = "Event[$name]"

}

// empty dispatch variant for Unit (void) events
fun Event<Unit>.dispatch(errLog: Logger = log) {
    dispatch(Unit, { l, e -> errLog.error("Error in $this listener ${l.name}", e) })
}

class EventListener<P>(
        val event: WeakReference<Event<P>>,
        val name: String?,
        val callback: (arg: P) -> Unit
) {
    fun detach() {
        event.get()?.listeners?.remove(this)
    }
    override fun toString() = "Event[$name]"
}
