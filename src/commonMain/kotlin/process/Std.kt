package org.jbali.process

/**
 * The standard IO streams: stdin, stdout and stderr
 */
enum class Std(
    /**
     * The file descriptor number of the stream:
     * - stdin / In: 0
     * - stdout / Out: 1
     * - stderr / Err: 2
     */
    val fd: UInt
) {
    In(0u),
    Out(1u),
    Err(2u);
    
    /**
     * "stdin", "stdout" or "stderr"
     */
    val stdname = "std${name.lowercase()}"
    
    val isInput = fd == 0u
    val isOutput = !isInput
    
    companion object {
        /**
         * Get the enum value from the file descriptor number:
         * - stdin / In: 0
         * - stdout / Out: 1
         * - stderr / Err: 2
         */
        fun fromFd(fd: UInt): Std =
            when (fd) {
                0u -> In
                1u -> Out
                2u -> Err
                else -> throw IllegalArgumentException("File descriptor $fd is not a standard IO stream")
            }
    }
    
}
