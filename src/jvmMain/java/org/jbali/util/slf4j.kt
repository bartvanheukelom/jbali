package org.jbali.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JvmName("loggerForClassKotlinReified") // not used by Java anyway
inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)
