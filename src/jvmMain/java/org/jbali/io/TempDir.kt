package org.jbali.io

import org.jbali.random.nextHex
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.random.Random


/**
 * [AutoCloseable] owner of a temporary directory, which is deleted on close.
 *
 * The primary constructor takes ownership of the given [dir], which must already exist.
 * @param deleteRecursively if true, delete the directory recursively on close, otherwise delete only the directory itself, which must be empty
 * @param ignoreCloseErrors if true, log and ignore deletion errors on close, otherwise throw them
 */
class TempDir(
    val dir: Path,
    private val deleteRecursively: Boolean = false,
    private val ignoreCloseErrors: Boolean = false,
) : AutoCloseable {
    
    private val log = LoggerFactory.getLogger(TempDir::class.java)
    
    // store the inode or whatever windows' equivalent is, so we can later check if the dir is still the same
    
    private val inode = dir.inodeOrNull()
    
    override fun toString() =
        "TempDir($dir)"
    
    override fun close() {
        doClose()
        if (ignoreCloseErrors) {
            try {
                doClose()
            } catch (e: Exception) {
                log.warn("Failed to delete $dir", e)
            }
        } else {
            doClose()
        }
    }
    
    @OptIn(ExperimentalPathApi::class)
    private fun doClose() {
        val nowInode = dir.inodeOrNull()
        if (nowInode != inode) {
            error("$dir changed inode from $inode to $nowInode")
        }
        if (deleteRecursively) {
            dir.deleteRecursively()
        } else {
            if (!dir.deleteIfExists()) {
                error("$dir no longer exists")
            }
        }
    }
}

inline fun TempDir.useDir(block: (Path) -> Unit) {
    block(dir)
}


/**
 * Create a new randomly named temporary directory in the appropriate location.
 * Currently this is /tmp on Linux.
 *
 * @param prefix if not null, prepend this to the random directory name with a hyphen
 * @throws IllegalStateException if /tmp is not a directory
 */
fun createFreeTempDir(
    prefix: String? = null,
): TempDir {
    
    val root = Path.of("/tmp") // TODO /dev/shm, windows support, etc.
    check(root.isDirectory()) { "$root is not a directory" }
    
    val random = Random.nextHex(12u)
    val name = prefix?.let { "$it-${random}" } ?: random
    val dir = root.resolve(name)
    
    // create (for now we're assuming collisions are near impossible)
    dir.createDirectory()
    // TODO race condition: here, another process could delete and recreate the dir, before TempDir takes the inode.
    //      could store a UUID in the attributes when creating?
    return TempDir(dir, deleteRecursively = true, ignoreCloseErrors = true)
    
}
