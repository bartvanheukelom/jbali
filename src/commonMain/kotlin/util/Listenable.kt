package org.jbali.util

interface ListenerReference {
    fun detach()
}


interface Listenable<out P> {

    fun listen(name: String?, callback: (arg: P) -> Unit): ListenerReference

    fun listen(callback: (arg: P) -> Unit) =
        listen(null, callback)

}
