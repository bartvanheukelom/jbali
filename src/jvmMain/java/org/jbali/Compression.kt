package org.jbali

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.jvm.JvmInline

@JvmInline
value class GZipped(val data: ByteArray) {
    val size get() = data.size

    fun unzipped(): ByteArray {
        GZIPInputStream(ByteArrayInputStream(data)).use {
            return it.readBytes()
        }
    }
}

fun ByteArray.gzipped(): GZipped {
    val bo = ByteArrayOutputStream()
    GZIPOutputStream(bo).use {
        it.write(this)
    }
    return GZipped(bo.toByteArray())
}
