package org.jbali.exposed

import org.jetbrains.exposed.sql.*


fun ColumnSet.referenceJoin(
    reference: Column<*>,
    joinType: JoinType,
    additionalConstraint: (SqlExpressionBuilder.() -> Op<Boolean>)? = null,
) =
    join(
        otherTable = reference.table, // TODO handle or make alternative for self-references (need alias)
        joinType = joinType,
        onColumn = reference.foreignKey!!.from,
        otherColumn = reference.foreignKey!!.target,
        additionalConstraint = additionalConstraint,
    )

infix fun ColumnSet.leftJoin(reference: Column<*>) = referenceJoin(reference, JoinType.LEFT)
infix fun ColumnSet.rightJoin(reference: Column<*>) = referenceJoin(reference, JoinType.RIGHT)
infix fun ColumnSet.innerJoin(reference: Column<*>) = referenceJoin(reference, JoinType.INNER)
