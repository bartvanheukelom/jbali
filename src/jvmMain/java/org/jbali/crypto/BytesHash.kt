package org.jbali.crypto

import java.security.MessageDigest

val ByteArray.sha256: ByteArray
    get() =
        MessageDigest.getInstance("SHA-256").digest(this)
