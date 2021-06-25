package org.jbali.exposed

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass

/**
 * Create/insert using [EntityClass.new],
 * then immediately [Entity.flush] it, so the actual `INSERT` query is performed.
 */
fun <ID : Comparable<ID>, T : Entity<ID>>
        EntityClass<ID, T>.insert
(
    init: T.() -> Unit
): T =
    new(init)
        .apply { flush() }
