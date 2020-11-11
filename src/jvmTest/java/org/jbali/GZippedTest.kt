package org.jbali

import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

class GZippedTest {

    @Test
    fun test() {

        val data = Random(12).nextBytes(1000)

        Assert.assertArrayEquals(data, data.gzipped().unzipped())

    }
}