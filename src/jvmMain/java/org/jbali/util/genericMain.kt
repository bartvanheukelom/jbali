package org.jbali.util

import org.jbali.sched.GlobalScheduler
import org.jbali.threads.ThreadPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import kotlin.concurrent.thread
import kotlin.system.exitProcess


val globalDaemonShutdownTrigger by lazy {
//    if (smoketest) {
//        DaemonShutdownTrigger.AfterInit
//    } else {
        DaemonShutdownTrigger.SigInt
//    }
}

/**
 * A main function that constructs and destroys the class named in the first program argument.
 */
object GenericMain {
    @JvmStatic fun main(args: Array<String>) {
        basicDaemonMain(
            shutdownTrigger = globalDaemonShutdownTrigger,
            mainAppCreator = {
                Class.forName(args[0]).newInstance() as Closeable
            }
        )
    }
}

enum class DaemonShutdownTrigger {
    /**
     * The daemon is shut down by a JVM shutdown hook, i.e. by SIGINT/Ctrl-C.
     */
    SigInt,
    /**
     * The daemon is shut down immediately after a succesful initialization, e.g. for smoketesting.
     */
    AfterInit,
    /**
     * The daemon is shut down when the returned callback is called, e.g. in unit tests.
     */
    Callback,
}

/**
 * Executes a basic daemon main function.
 *
 * @param shutdownTrigger the trigger for shutting down the daemon (default value: DaemonShutdownTrigger.SigInt)
 * @param requireCleanShutdown a boolean indicating if a clean shutdown is required (default value: true if the shutdown trigger is not DaemonShutdownTrigger.SigInt, false otherwise)
 * @param onShutdown a callback function to be executed on shutdown (default value: an empty function)
 * @param mainAppCreator a function that creates the main application (must return a [Closeable] object)
 *
 * @return a callback function that can be used to shut down the daemon if the shutdown trigger is [DaemonShutdownTrigger.Callback]. If the shutdown trigger is not [DaemonShutdownTrigger.Callback], the callback function will throw [IllegalArgumentException].
 */
fun basicDaemonMain(
    shutdownTrigger: DaemonShutdownTrigger = DaemonShutdownTrigger.SigInt,
    requireCleanShutdown: Boolean = shutdownTrigger != DaemonShutdownTrigger.SigInt,
    keepGlobalsReusable: Boolean = false,
    onShutdown: () -> Unit = {},
    mainAppCreator: () -> Closeable
): () -> Unit {
    
    var log: Logger? = null
    var jvmShutdownStarted = false
    
    /**
     * Exit the JVM, using halt if required.
     */
    fun exit(s: Int): Nothing {
        if (jvmShutdownStarted) {
            Runtime.getRuntime().halt(s)
        }
        // this can never run if halt does, but the compiler doesn't know halt returns Nothing
        exitProcess(s)
    }
    
    /**
     * Try to log the given error, then exit the JVM.
     */
    fun errorExit(e: Throwable, during: String): Nothing {
        
        try {
            if (log != null) {
                log!!.error("Error in main during $during", e)
            } else {
                e.printStackTrace()
            }
            
            // global pools don't _need_ to be shut down if we're about to exit,
            // but calling this can provide helpful diagnostics.
            if (!keepGlobalsReusable) {
                shutdownGlobalPools()
            }
            
            exit(1)
        } catch (ee: Throwable) {
            // meh
            try {
                ee.printStackTrace()
            } catch (eee: Throwable) {}
            exit(111)
        }
    }
    
    try {
        
        log = LoggerFactory.getLogger("main")
        log.info("^^^^^^^^^^^^^^^^^^^^^^ [start] ^^^^^^^^^^^^^^^^^^^^^^")
        
        val app =
            try {
                mainAppCreator()
            } catch (e: Throwable) {
                errorExit(e, "mainAppCreator()")
            }
        
        log.info("^^^^^^^^^^^^^^^^^^^^^^ [inited] ^^^^^^^^^^^^^^^^^^^^^^")
        
        fun shutdown(by: String, extraException: Throwable? = null) {
            try {
                log.info("$$$$$$$$$$$$$$$$$$$$$$ [shutting down by $by] $$$$$$$$$$$$$$$$$$$$$$")
                app.close()
                onShutdown()
                if (!keepGlobalsReusable) {
                    shutdownGlobalPools()
                }
                log.info("##################### [ready for termination] #######################")
                
                val checkerName = "lingering thread checker"
                thread(
                    name = checkerName,
                    isDaemon = true
                ) {
                    Thread.sleep(5000L)
                    log.warn("Something is keeping the process alive. Going to log all non-daemon threads.")
                    Thread.getAllStackTraces()
                        .filterKeys { !it.isDaemon }
                        .forEach { (t, u) ->
                            log.warn(t.name, Throwable().apply { stackTrace = u })
                        }
                    val exitCode = if (requireCleanShutdown) 111 else 0 // there is no special significance to 111
                    log.warn("Now exiting forcefully with status $exitCode")
                    exitProcess(exitCode)
                }
            } catch (e: Throwable) {
                extraException?.let(e::addSuppressed)
                errorExit(e, "shutdown by $by")
            }
        }
        
        when (shutdownTrigger) {
            DaemonShutdownTrigger.SigInt -> {
                // gracefully shut down on Ctrl-C / SIGTERM
                // TODO or should we join the shutdown thread then run shutdown next (keeping main thread suspended the whole lifetime).
                // TODO also, how does JVM respond to ctrl-c on main thread.
                @Suppress("ThrowableNotThrown")
                val hookRegisterStack = Exception("Shutdown hook registered here")
                Runtime.getRuntime().addShutdownHook(Thread {
                    jvmShutdownStarted = true
                    try {
                        shutdown("shutdownHook", hookRegisterStack)
                    } catch (e: Throwable) {
                        e.addSuppressed(hookRegisterStack)
                    }
                })
            }
            DaemonShutdownTrigger.AfterInit ->
                // TODO add a deamon thread that exits after timeout
                shutdown("shutDownAfterInit")
            DaemonShutdownTrigger.Callback -> {}
        }
        
        return {
            require(shutdownTrigger == DaemonShutdownTrigger.Callback)
            shutdown("callback")
        }
        
    } catch (e: Throwable) {
        errorExit(e, "other")
    }
    
}

// TODO this can be done better, e.g. make each non-active pooled thread a deamon thread
fun shutdownGlobalPools() {
    ThreadPool.shutdown()
    GlobalScheduler.shutdownNow()
}
