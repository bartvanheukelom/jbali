package org.jbali.coroutines


/**
 * IntelliJ IDEA can use this to detect when blocking code is inappropriately
 * called in a suspending context.
 *
 * For this to work, add the name of this annotation to the settings of the
 * "Inappropriate thread-blocking method call" inspection.
 */
annotation class Blocking

/**
 * See [Blocking].
 */
annotation class NonBlocking
