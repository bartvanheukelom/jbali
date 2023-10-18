package org.jbali.util

import org.jbali.events.call
import org.jbali.events.changedTo
import org.jbali.events.listenOnce
import org.jbali.events.smartListen
import org.slf4j.Logger

@Deprecated("use org.jbali.events.Observable")
typealias Observable<T> = org.jbali.events.Observable<T>
@Deprecated("use org.jbali.events.changedTo")
infix fun <T> T.changedTo(after: T): Change<T> = this.changedTo(after)

@Deprecated("use org.jbali.events.Change")
typealias Change<T> = org.jbali.events.Change<T>
@Deprecated("use org.jbali.events.MutableObservable")
typealias MutableObservable<T> = org.jbali.events.MutableObservable<T>


@Deprecated("use org.jbali.events.ListenerErrorCallback")
typealias ListenerErrorCallback<P> = org.jbali.events.ListenerErrorCallback<P>
@Deprecated("use org.jbali.events.EventDelegate")
typealias EventDelegate<P> = org.jbali.events.EventDelegate<P>
@Deprecated("use org.jbali.events.OnceEventDelegate")
typealias OnceEventDelegate<P> = org.jbali.events.OnceEventDelegate<P>
@Deprecated("use org.jbali.events.SmartListenerResult")
typealias SmartListenerResult = org.jbali.events.SmartListenerResult
@Deprecated("use org.jbali.events.StopListening")
typealias StopListening = org.jbali.events.StopListening
@Deprecated("use org.jbali.events.KeepListening")
typealias KeepListening = org.jbali.events.KeepListening
@Deprecated("use org.jbali.events.smartListen")
fun <P> Listenable<P>.smartListen(name: String, callback: (arg: P) -> SmartListenerResult): ListenerReference =
    smartListen(name, callback)
@Deprecated("use org.jbali.events.listenOnce")
fun <P> Listenable<P>.listenOnce(name: String, callback: (arg: P) -> Unit) =
    listenOnce(name, callback)
@Deprecated("use org.jbali.events.Event")
typealias Event<P> = org.jbali.events.Event<P>
@Deprecated("use org.jbali.events.OnceEvent")
typealias OnceEvent<P> = org.jbali.events.OnceEvent<P>
@Deprecated("use org.jbali.events.EventHandler")
typealias EventHandler<P> = org.jbali.events.EventHandler<P>
@Deprecated("use org.jbali.events.EventAssertionError")
typealias EventAssertionError = org.jbali.events.EventAssertionError
@Deprecated("use org.jbali.events.EventListener")
typealias EventListener<P> = org.jbali.events.EventListener<P>
@Deprecated("use org.jbali.events.detach")
fun Iterable<ListenerReference>.detach() = this.forEach { it.detach() }

@Deprecated("use org.jbali.events.dispatch")
fun Event<Unit>.dispatch(errCb: ListenerErrorCallback<Unit> = ListenerErrorCallbacks.default) {
    dispatch(Unit, errCb)
}

@Deprecated("use org.jbali.events.dispatch")
fun Event<Unit>.dispatch(errLog: Logger = ListenerErrorCallbacks.defaultErrorLog) {
    dispatch(Unit, ListenerErrorCallbacks.logging(errLog))
}

@Deprecated("use org.jbali.events.dispatch")
fun Event<Unit>.dispatch() {
    dispatch(Unit, ListenerErrorCallbacks.default)
}
@Deprecated("use org.jbali.events.call")
fun EventListener<Unit>.call(errCb: ListenerErrorCallback<Unit> = ListenerErrorCallbacks.default) {
    call(errCb)
}
@Deprecated("use org.jbali.events.ListenerErrorCallbacks")
typealias ListenerErrorCallbacks = org.jbali.events.ListenerErrorCallbacks

@Deprecated("use org.jbali.events.FatalEventError")
typealias FatalEventError = org.jbali.events.FatalEventError

@Deprecated("use org.jbali.events.Listenable")
typealias Listenable<P> = org.jbali.events.Listenable<P>
@Deprecated("use org.jbali.events.ListenerReference")
typealias ListenerReference = org.jbali.events.ListenerReference