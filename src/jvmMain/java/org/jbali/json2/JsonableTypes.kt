package org.jbali.json2


/**
 * Describes a native Kotlin type that is 1:1 representable in JSON, i.e. a union of:
 * - [JsonableMap] ([Map]<[String], [JsonableValue]?>)
 * - [JsonableList] ([List]<[JsonableValue]?>)
 * - [JsonableLiteral] ([String] | [Boolean] | [Double])
 *
 * This union does not include `null` which, though a valid JSON type, can instead be assigned to `JsonValue?`.
 *
 * Since Kotlin doesn't support union types, for the compiler this is a typealias for [Any].
 * As such, it mostly serves as documentation. A function which takes a value of this type may be expected
 * to perform runtime checking on the value.
 *
 * https://discuss.kotlinlang.org/t/union-types/77
 *
 * TODO inline class? but should not be stored in Map/List boxed. what value does it add over JsonElement?
 */
typealias JsonableValue = Any

/**
 * Describes a native Kotlin type that is 1:1 representable as JSON literal, i.e. a union of:
 * - [String]
 * - [Boolean]
 * - [Double]
 *
 * This union does not include `null` which, though a valid JSON type, can instead be assigned to `JsonableLiteral?`.
 *
 * Since Kotlin doesn't support union types, for the compiler this is a typealias for [Any].
 * As such, it mostly serves as documentation. A function which takes a value of this type may be expected
 * to perform runtime checking on the value.
 *
 * @see JsonableValue
 */
typealias JsonableLiteral = Any

typealias JsonableMap = Map<String, JsonableValue?>
typealias JsonableList = List<JsonableValue?>
