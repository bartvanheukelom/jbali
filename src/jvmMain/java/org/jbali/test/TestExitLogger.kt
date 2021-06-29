package org.jbali.test

import org.slf4j.LoggerFactory
import java.time.Instant

object TestExitLogger {
    
    private val primer by lazy {
        val log = LoggerFactory.getLogger(TestExitLogger::class.java)
        
        Runtime.getRuntime().addShutdownHook(Thread {
            log.info("Test JVM shutting down at ${Instant.now()}")
        })
    }
    
    fun prime() {
        primer
    }
    
}
