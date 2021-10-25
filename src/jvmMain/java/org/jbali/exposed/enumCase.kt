package org.jbali.exposed

import org.jetbrains.exposed.sql.*
import kotlin.reflect.KClass

@Suppress("unused") // receiver
inline fun <reified E : Enum<E>, R> SqlExpressionBuilder.enumCase(
    valueExpression: Expression<E>,
    elseResult: Expression<R>? = null,
    noinline results: (E) -> Expression<R>?
) =
    EnumCase(
        enumClass = E::class,
        valueExpression = valueExpression,
        elseResult = elseResult,
        results = results,
    )

class EnumCase<E : Enum<E>, R>(
    private val enumClass: KClass<E>,
    private val valueExpression: Expression<E>,
    private val elseResult: Expression<R>? = null,
    private val results: (E) -> Expression<R>?,
): Op<R>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("CASE ")
        +valueExpression
    
        for (v in enumClass.java.enumConstants!!) {
            results(v)?.let { res ->
                append(" WHEN ")
                append(v.sqlLiteral)
                append(" THEN ")
    
                // exposed doesn't apply this check in this case
                // TODO should it? then report. or am I using it wrong?
                if (res is ComplexExpression) {
                    append("(", res, ")")
                } else {
                    append(res)
                }
            }
        }
    
        if (elseResult != null) {
            append(" ELSE ")
            // TODO see above
            // TODO dedup
            if (elseResult is ComplexExpression) {
                append("(", elseResult, ")")
            } else {
                append(elseResult)
            }
        }
        
        append(" END")
    }
}
