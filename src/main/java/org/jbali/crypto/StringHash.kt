package org.jbali.crypto

val String.sha256: ByteArray
    get() = this.toByteArray().sha256
