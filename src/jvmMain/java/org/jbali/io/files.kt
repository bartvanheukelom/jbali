package org.jbali.io

import org.jbali.random.nextHex
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.random.Random

//.let { ser ->
//    file.parentFile.mkdirs()
//
//    // new filename
//    val outFile = file.resolveSibling("~${file.nameWithoutExtension}.${Instant.now().toEpochMilli()}.${file.extension}")
//
//    // write to new file
//    outFile.writeText(ser)
//
//    // atomic swap
//    Files.move(outFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
//
//    // NOTE: if this fails, we're leaving the (partial) output file behind for recovery/inspection
//}
//
//


/**
 * Generates a temporary file name adjacent to this filename,
 * calls [writer] on it, then atomically renames the temporary file to this filename,
 * replacing any existing file (if supported, see [Files.move]).
 *
 * If writing or renaming fails, the temporary file is left behind for recovery/inspection.
 */
fun File.writeAtomically(writer: (File) -> Unit) {
    val outFile = this.resolveSibling("~${this.nameWithoutExtension}.${Instant.now().toEpochMilli()}_${Random.nextHex(4u)}.${this.extension}")
    writer(outFile)
    Files.move(outFile.toPath(), this.toPath(), StandardCopyOption.ATOMIC_MOVE)
}


/**
 * On supported POSIX systems, returns the inode of this file.
 * Returns `null` if the file system does not support inodes.
 *
 * @throws IOException see [Files.getAttribute]
 * @throws SecurityException see [Files.getAttribute]
 */
@Throws(IOException::class)
fun Path.inodeOrNull(): Long? =
    try {
        Files.getAttribute(this, "unix:ino") as Long
    } catch (e: UnsupportedOperationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
