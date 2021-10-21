package org.jbali.jmsrpc

import kotlinx.serialization.Serializable

@Serializable
data class TestKoseData(
    val fibonacciMaybe: List<Int>
)

data class TestJavaSerData(
    val foo: String
) : java.io.Serializable {
    companion object {
        @JvmField
        val INSTANCE = TestJavaSerData("bar")
    }
}