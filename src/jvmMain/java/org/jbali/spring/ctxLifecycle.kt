package org.jbali.spring

import org.jbali.util.DaemonShutdownTrigger
import org.jbali.util.basicDaemonMain
import org.jbali.util.globalDaemonShutdownTrigger
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.GenericApplicationContext
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass


/**
 * A main function that constructs and destroys a spring context with the class named in the first program argument as config.
 */
object SpringMain {
    @JvmStatic fun main(args: Array<String>) {
        springMain(
            config = Class.forName(args[0]).kotlin,
            shutdownTrigger = globalDaemonShutdownTrigger,
        )
    }
}

/**
 * A main function that constructs and destroys a spring context with the given class as config.
 * @return callback that can be used to shut down the daemon, if [shutdownTrigger] is [DaemonShutdownTrigger.Callback].
 *         If it's not, the callback will throw [IllegalArgumentException] (TODO IllegalStateException)
 */
fun springMain(
    config: KClass<*>,
    shutdownTrigger: DaemonShutdownTrigger = DaemonShutdownTrigger.SigInt
): () -> Unit =
    basicDaemonMain(
        shutdownTrigger = shutdownTrigger,
        mainAppCreator = {
            AnnotationConfigApplicationContext(config.java)
                .started()
//                .runSmokeTests()
                .asStoppingCloseable()  // TODO exceptions in destroy methods are logged and ignored. fine for service, but in smoketest this should result in a non-0 exit code.
        }
    )

/**
 * Start this context and return it.
 */
fun GenericApplicationContext.started() =
    apply { start() }

/**
 * Start this context, run [user], then stop and close it.
 */
fun <T> GenericApplicationContext.useStarted(user: (GenericApplicationContext) -> T): T {
    start()
    return asStoppingCloseable().use {
        user(this)
    }
}


/**
 * Return a [Closeable] that, when closed, first _stops_ and then closes this context.
 */
fun GenericApplicationContext.asStoppingCloseable() =
    object : Closeable {
        
        private val ctx: GenericApplicationContext = this@asStoppingCloseable
        private val closed: AtomicReference<Throwable?> = AtomicReference()
        
        override fun close() {
            
            if (!closed.compareAndSet(null, Exception("Was closed here"))) {
                throw IllegalStateException("Was already closed").also {
                    it.addSuppressed(closed.get())
                }
            }
            
            // at this point we've already been "using" the context for a while.
            // we just use use (...) for making sure ctx is closed even if stop throws
            ctx.use { ctx ->
                ctx.stop()
            }
        }
    }
