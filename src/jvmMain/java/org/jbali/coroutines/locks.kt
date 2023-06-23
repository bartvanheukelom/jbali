package org.jbali.coroutines

import kotlinx.coroutines.sync.Mutex


/**
 * Attempts to unlock the mutex if it is currently held by the specified owner.
 *
 * The method is beneficial in scenarios where there may be ambiguity about the current owner of the lock.
 * This typically occurs in complex systems or when writing fallbacks and low-maintenance code where the
 * control flow may be hard to predict or track.
 *
 * Example usage:
 * ```
 * lk.lock(me)
 * try {
 *    doStuff()
 *    lk.unlock()  // early release of the lock for important reasons
 *    doMoreStuff()
 * } finally {
 *    // did we already release the lock? who knows? who cares!
 *    lk.unlockIfHeld(me)
 * }
 * ```
 *
 * @param owner The owner for which the lock status will be checked.
 * @return true if the mutex was successfully unlocked, false if the mutex was not held by the owner.
 * @throws IllegalStateException if the mutex is locked, but not by the specified owner.
 */
fun Mutex.unlockIfHeld(owner: Any): Boolean {
    when {
        holdsLock(owner) -> {
            unlock(owner)
            return true
        }
        isLocked -> {
            throw IllegalStateException("Mutex is locked, but not by the specified owner")
        }
        else -> {
            return false
        }
    }
}
