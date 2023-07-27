package org.jbali.json2


interface ToTsContext {
    fun define(namedType: TsNamedType)
}

/**
 * ToTsContext that simply won't define any named types that are referenced.
 */
class ToStringToTsContext : ToTsContext {
    override fun define(namedType: TsNamedType) {
        // blah
    }
}

// TODO need at least 2 classes: type use and type definition. what about generics?
sealed class TsType {
    
    context(ToTsContext)
    abstract fun toTs(): String
    
    override fun toString() = with(ToStringToTsContext()) {
        toTs()
    }
    
}

// TODO perhaps make all of these a subtype of TsNamedType, with intrinsic definitions
object TsAny : TsType() {
    override fun toTs() = "any"
}
object TsString : TsType() {
    override fun toTs() = "string"
}
object TsNumber : TsType() {
    override fun toTs() = "number"
}
object TsBoolean : TsType() {
    override fun toTs() = "boolean"
}
object TsNull : TsType() {
    override fun toTs() = "null"
}
object TsUndefined : TsType() {
    override fun toTs() = "undefined"
}
object TsUnknown : TsType() {
    override fun toTs() = "unknown"
}


class TsArray(val itemType: TsType) : TsType() {
    context(ToTsContext)
    override fun toTs() = "${itemType.toTs()}[]"
}
class TsObject(val properties: Map<String, TsType>) : TsType() {
    context(ToTsContext)
    override fun toTs() = buildString {
        appendLine("{")
        properties.forEach { (name, type) ->
            appendLine("    $name: ${type.toTs()};")
        }
        append("}")
    }
}


class TsNamedType(val name: String, val definition: TsType) : TsType() {
    context(ToTsContext)
    override fun toTs(): String {
        define(this)
        return name
    }
}
fun TsType.named(name: String) = TsNamedType(name, this)
// TODO interfaces


class TsUnion(val types: List<TsType>) : TsType() {
    context(ToTsContext)
    override fun toTs() = types.joinToString(" | ") { it.toTs() }
}
fun TsType.union(other: TsType) = TsUnion(listOf(this, other))
infix fun TsType.u(other: TsType) = TsUnion(listOf(this, other))


class TsIntersection(val types: List<TsType>) : TsType() {
    context(ToTsContext)
    override fun toTs() = types.joinToString(" & ") { it.toTs() }
}
fun TsType.intersect(other: TsType) = TsIntersection(listOf(this, other))
infix fun TsType.i(other: TsType) = TsIntersection(listOf(this, other))


fun TsNamedType.toDefinitionsFile(): String = buildString {
    val defined = mutableMapOf<String, TsNamedType>()
    val toDefine = ArrayDeque<TsNamedType>()
    
    val ctx = object : ToTsContext {
        override fun define(namedType: TsNamedType) {
            val existing = defined[namedType.name]
            if (existing != null) {
                if (existing.definition != namedType.definition) {
                    throw IllegalArgumentException("Type ${namedType.name} already defined as ${existing.definition}, cannot redefine as ${namedType.definition}")
                }
            } else {
                toDefine.add(namedType)
            }
        }
    }
    
    with (ctx) {
        define(this@toDefinitionsFile)
        while (toDefine.isNotEmpty()) {
            val td = toDefine.removeFirst()
            // add to defined first, so that recursive references don't cause infinite loops
            defined[td.name] = td
            appendLine("export type ${td.name} = ${td.definition.toTs()};")
        }
    }
}
