package org.jbali.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

inline fun <E> CoroutineScope.parallelReceive(
    channel: ReceiveChannel<E>,
    receivers: UInt = 16u,
    crossinline action: suspend (E) -> Unit,
) {
    repeat(receivers.toInt()) {
        launch {
            while (true) {
                action(channel.receive())
            }
        }
    }
}
