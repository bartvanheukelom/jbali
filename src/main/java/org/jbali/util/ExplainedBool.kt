package org.jbali.util

data class ExplainedBool @JvmOverloads constructor(
        val value: Boolean,
        val explanation: String = "byDefault"
) {
    constructor(value: Boolean, explanationRequested: Boolean, explainer: () -> String) :
            this(
                    value = value,
                    explanation = if (explanationRequested) explainer() else "<explanation not requested>"
            )

    override fun toString() = "${value.toString().padEnd(5)} because $explanation"

    /** Because `if (foo().isTrue())` reads better than `if (foo().value)` */
    fun isTrue() = value
    fun isFalse() = !value

    /**
     * Allows the following usage pattern:
     * ```
     * var exp = OutVar<String>()
     * return
     *   if (explainedFoo().isFalse(exp)) false because "foo said $exp"
     *   else (explainedBar().isTrue(exp)) true because "bar said $exp"
     *   else true because "why not"
     * }
     * ```
     */
    fun isTrue(outExplanation: OutVar<String>): Boolean {
        outExplanation.value = explanation
        return value
    }

    /**
     * See isTrue for doc in reverse
     */
    fun isFalse(outExplanation: OutVar<String>): Boolean {
        outExplanation.value = explanation
        return !value
    }

}


infix fun Boolean.because(explanation: String) = ExplainedBool(this, explanation)
fun Boolean.byDefault() = ExplainedBool(this)

