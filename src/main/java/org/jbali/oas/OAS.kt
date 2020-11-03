package org.jbali.oas

import kotlinx.serialization.*; import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

// https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md
// https://github.com/OAI/OpenAPI-Specification/blob/master/schemas/v3.0/schema.json
// https://app.quicktype.io/?l=kotlin

/**
 * Validation schema for OpenAPI Specification 3.0.X.
 */
@Serializable
data class OpenAPI (
        val components: Components? = null,
        val externalDocs: ExternalDocumentation? = null,
        val info: Info,
        val openapi: String,
        val paths: Map<String, PathItem>,
        val security: List<Map<String, List<String>>>? = null,
        val servers: List<Server>? = null,
        val tags: List<Tag>? = null
)

@Serializable
data class Components (
//        val callbacks: Map<String, Map<String, Callback>>? = null,
        val examples: Map<String, Example>? = null,
        val headers: Map<String, HeaderValue>? = null,
        val links: Map<String, Link>? = null,
        val parameters: Map<String, Parameter>? = null,
        val requestBodies: Map<String, RequestBody>? = null,
        val responses: Map<String, Response>? = null,
        val schemas: Map<String, SchemaElement>? = null,
        val securitySchemes: Map<String, SecurityScheme>? = null
)

@Serializable
data class Operation (
//        val callbacks: Map<String, Map<String, Callback>>? = null,
        val deprecated: Boolean? = null,
        val description: String? = null,
        val externalDocs: ExternalDocumentation? = null,

        @SerialName("operationId")
        val operationID: String? = null,

        val parameters: List<Parameter>? = null,
        val requestBody: RequestBody? = null,
        val responses: Map<String, Response>,
        val security: List<Map<String, List<String>>>? = null,
        val servers: List<Server>? = null,
        val summary: String? = null,
        val tags: List<String>? = null
)

@Serializable
data class PathItem (
        val delete: Operation? = null,
        val description: String? = null,
        val get: Operation? = null,
        val head: Operation? = null,
        val options: Operation? = null,
        val parameters: List<Parameter>? = null,
        val patch: Operation? = null,
        val post: Operation? = null,
        val put: Operation? = null,
        val servers: List<Server>? = null,
        val summary: String? = null,
        val trace: Operation? = null
)

sealed class Callback {
    class PathItemValue(val value: PathItem) : Callback()
    class StringValue(val value: String)     : Callback()
}

@Serializable
data class ExternalDocumentation (
        val description: String? = null,
        val url: String
)

@Serializable
data class Parameter (
//        val allowEmptyValue: AllowEmptyValue? = null,
//        val allowReserved: AllowEmptyValue? = null,
//        val content: Content? = null,
//        val deprecated: AllowEmptyValue? = null,
        val description: String? = null,
        val example: JsonObject? = null,
//        val examples: Examples? = null,
//        val explode: AllowEmptyValue? = null,

        @SerialName("in")
        val parameterIn: String? = null,

        val name: String? = null,
//        val required: AllowEmptyValue? = null,
//        val schema: SchemaUnion? = null,
        val style: String? = null
)

sealed class AllowEmptyValue {
    class BoolValue(val value: Boolean)  : AllowEmptyValue()
    class StringValue(val value: String) : AllowEmptyValue()
}

sealed class Content {
    class MediaTypeMapValue(val value: Map<String, MediaType>) : Content()
    class StringValue(val value: String)                       : Content()
}

@Serializable
data class Header (
        val allowEmptyValue: Boolean? = null,
        val allowReserved: Boolean? = null,
        val content: Map<String, MediaType>? = null,
        val deprecated: Boolean? = null,
        val description: String? = null,
        val example: JsonObject? = null,
        val examples: Map<String, Example>? = null,
        val explode: Boolean? = null,
        val required: Boolean? = null,
        val schema: SchemaElement? = null,
        val style: HeaderStyle? = null
)

@Serializable
data class Encoding (
        val allowReserved: Boolean? = null,
        val contentType: String? = null,
        val explode: Boolean? = null,
        val headers: Map<String, Header>? = null,
        val style: EncodingStyle? = null
)

/**
 * Example and examples are mutually exclusive
 */
@Serializable
data class MediaType (
        val encoding: Map<String, Encoding>? = null,
        val example: JsonObject? = null,
        val examples: Map<String, Example>? = null,
        val schema: SchemaElement? = null
)

@Serializable
data class Example (
        val description: String? = null,
        val externalValue: String? = null,
        val summary: String? = null,
        val value: JsonObject? = null,

        @SerialName("\$ref")
        val ref: String? = null
)

sealed class Properties {
    class SchemaElementMapValue(val value: Map<String, SchemaElement>) : Properties()
    class StringValue(val value: String)                               : Properties()
}

sealed class SchemaUnion {
    class SchemaElementValue(val value: SchemaElement) : SchemaUnion()
    class StringValue(val value: String)               : SchemaUnion()
}

sealed class Of {
    class SchemaElementArrayValue(val value: List<SchemaElement>) : Of()
    class StringValue(val value: String)                          : Of()
}

@Serializable
data class Schema (
//        val additionalProperties: AdditionalProperties? = null,
//        val allOf: Of? = null,
//        val anyOf: Of? = null,
        val default: JsonObject? = null,
//        val deprecated: AllowEmptyValue? = null,
        val description: String? = null,
//        val discriminator: DiscriminatorUnion? = null,
//        val enum: EnumUnion? = null,
        val example: JsonObject? = null,
//        val exclusiveMaximum: AllowEmptyValue? = null,
//        val exclusiveMinimum: AllowEmptyValue? = null,
//        val externalDocs: ExternalDocs? = null,
        val format: String? = null,
//        val items: SchemaUnion? = null,
//        val maximum: Imum? = null,
//        val maxItems: MaxItems? = null,
//        val maxLength: MaxItems? = null,
//        val maxProperties: MaxItems? = null,
//        val minimum: Imum? = null,
//        val minItems: MaxItems? = null,
//        val minLength: MaxItems? = null,
//        val minProperties: MaxItems? = null,
//        val multipleOf: MultipleOf? = null,
//        val not: SchemaUnion? = null,
//        val nullable: AllowEmptyValue? = null,
//        val oneOf: Of? = null,
        val pattern: String? = null,
//        val properties: Properties? = null,
//        val readOnly: AllowEmptyValue? = null,
//        val required: Required? = null,
        val title: String? = null,
        val type: String? = null
//        val uniqueItems: AllowEmptyValue? = null,
//        val writeOnly: AllowEmptyValue? = null,
//        val xml: XMLUnion? = null
)

sealed class AdditionalProperties {
    class BoolValue(val value: Boolean)  : AdditionalProperties()
    class SchemaValue(val value: Schema) : AdditionalProperties()
    class StringValue(val value: String) : AdditionalProperties()
}

@Serializable
data class SchemaElement (
//        val additionalProperties: AdditionalProperties? = null,
//        val allOf: Of? = null,
//        val anyOf: Of? = null,
        val default: JsonObject? = null,
//        val deprecated: AllowEmptyValue? = null,
        val description: String? = null,
//        val discriminator: DiscriminatorUnion? = null,
//        val enum: EnumUnion? = null,
        val example: JsonObject? = null,
//        val exclusiveMaximum: AllowEmptyValue? = null,
//        val exclusiveMinimum: AllowEmptyValue? = null,
//        val externalDocs: ExternalDocs? = null,
        val format: String? = null,
//        val items: SchemaUnion? = null,
//        val maximum: Imum? = null,
//        val maxItems: MaxItems? = null,
//        val maxLength: MaxItems? = null,
//        val maxProperties: MaxItems? = null,
//        val minimum: Imum? = null,
//        val minItems: MaxItems? = null,
//        val minLength: MaxItems? = null,
//        val minProperties: MaxItems? = null,
//        val multipleOf: MultipleOf? = null,
//        val not: SchemaUnion? = null,
//        val nullable: AllowEmptyValue? = null,
//        val oneOf: Of? = null,
        val pattern: String? = null,
//        val properties: Properties? = null,
//        val readOnly: AllowEmptyValue? = null,
//        val required: Required? = null,
        val title: String? = null,
        val type: String? = null
//        val uniqueItems: AllowEmptyValue? = null,
//        val writeOnly: AllowEmptyValue? = null,
//        val xml: XMLUnion? = null,

)

sealed class DiscriminatorUnion {
    class DiscriminatorValue(val value: Discriminator) : DiscriminatorUnion()
    class StringValue(val value: String)               : DiscriminatorUnion()
}

@Serializable
data class Discriminator (
        val mapping: Map<String, String>? = null,
        val propertyName: String
)

sealed class EnumUnion {
    class AnythingArrayValue(val value: JsonArray) : EnumUnion()
    class StringValue(val value: String)           : EnumUnion()
}

sealed class ExternalDocs {
    class ExternalDocumentationValue(val value: ExternalDocumentation) : ExternalDocs()
    class StringValue(val value: String)                               : ExternalDocs()
}

sealed class MaxItems {
    class IntegerValue(val value: Long)  : MaxItems()
    class StringValue(val value: String) : MaxItems()
}

sealed class Imum {
    class DoubleValue(val value: Double) : Imum()
    class StringValue(val value: String) : Imum()
}

sealed class MultipleOf {
    class DoubleValue(val value: Double) : MultipleOf()
    class StringValue(val value: String) : MultipleOf()
}

sealed class Required {
    class StringArrayValue(val value: List<String>) : Required()
    class StringValue(val value: String)            : Required()
}

sealed class XMLUnion {
    class StringValue(val value: String) : XMLUnion()
    class XMLValue(val value: XML)       : XMLUnion()
}

@Serializable
data class XML (
        val attribute: Boolean? = null,
        val name: String? = null,
        val namespace: String? = null,
        val prefix: String? = null,
        val wrapped: Boolean? = null
)

@Serializable(with = HeaderStyle.Companion::class)
enum class HeaderStyle(val value: String) {
    Simple("simple");

    companion object : KSerializer<HeaderStyle> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveDescriptor("quicktype.HeaderStyle", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): HeaderStyle = when (val value = decoder.decodeString()) {
            "simple" -> Simple
            else     -> throw IllegalArgumentException("HeaderStyle could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: HeaderStyle) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable(with = EncodingStyle.Companion::class)
enum class EncodingStyle(val value: String) {
    DeepObject("deepObject"),
    Form("form"),
    PipeDelimited("pipeDelimited"),
    SpaceDelimited("spaceDelimited");

    companion object : KSerializer<EncodingStyle> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveDescriptor("quicktype.EncodingStyle", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): EncodingStyle = when (val value = decoder.decodeString()) {
            "deepObject"     -> DeepObject
            "form"           -> Form
            "pipeDelimited"  -> PipeDelimited
            "spaceDelimited" -> SpaceDelimited
            else             -> throw IllegalArgumentException("EncodingStyle could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: EncodingStyle) {
            return encoder.encodeString(value.value)
        }
    }
}

sealed class Examples {
    class ExampleMapValue(val value: Map<String, Example>) : Examples()
    class StringValue(val value: String)                   : Examples()
}

@Serializable
data class RequestBody (
//        val content: Content? = null,
        val description: String? = null,
//        val required: AllowEmptyValue? = null,

        @SerialName("\$ref")
        val ref: String? = null
)

/**
 * [https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#responseObject]
 */
@Serializable
data class Response (
        val content: JsonObject? = null,
        val description: String? = null,
//        val headers: Headers? = null,
//        val links: Links? = null,

        @SerialName("\$ref")
        val ref: String? = null
)

sealed class Headers {
    class HeaderValueMapValue(val value: Map<String, HeaderValue>) : Headers()
    class StringValue(val value: String)                           : Headers()
}

@Serializable
data class HeaderValue (
//        val allowEmptyValue: AllowEmptyValue? = null,
//        val allowReserved: AllowEmptyValue? = null,
//        val content: Content? = null,
//        val deprecated: AllowEmptyValue? = null,
        val description: String? = null,
        val example: JsonObject? = null,
//        val examples: Examples? = null,
//        val explode: AllowEmptyValue? = null,
//        val required: AllowEmptyValue? = null,
//        val schema: SchemaUnion? = null,
        val style: String? = null,

        @SerialName("\$ref")
        val ref: String? = null
)

sealed class Links {
    class LinkMapValue(val value: Map<String, Link>) : Links()
    class StringValue(val value: String)             : Links()
}

@Serializable
data class Link (
        val description: String? = null,

        @SerialName("operationId")
        val operationID: String? = null,

        val operationRef: String? = null,
//        val parameters: Parameters? = null,
        val requestBody: JsonObject? = null,
//        val server: ServerUnion? = null,

        @SerialName("\$ref")
        val ref: String? = null
)

sealed class Parameters {
    class AnythingMapValue(val value: JsonObject) : Parameters()
    class StringValue(val value: String)          : Parameters()
}

sealed class ServerUnion {
    class ServerValue(val value: Server) : ServerUnion()
    class StringValue(val value: String) : ServerUnion()
}

@Serializable
data class Server (
        val description: String? = null,
        val url: String,
        val variables: Map<String, ServerVariable>? = null
)

@Serializable
data class ServerVariable (
        val default: String,
        val description: String? = null,
        val enum: List<String>? = null
)

@Serializable
data class SecurityScheme (
        @SerialName("\$ref")
        val ref: String? = null,

        val description: String? = null,

        @SerialName("in")
        val securitySchemeIn: String? = null,

        val name: String? = null,
        val type: String? = null,
        val bearerFormat: String? = null,
        val scheme: String? = null,
//        val flows: Flows? = null,

        @SerialName("openIdConnectUrl")
        val openIDConnectURL: String? = null
)

sealed class Flows {
    class OAuthFlowsValue(val value: OAuthFlows) : Flows()
    class StringValue(val value: String)         : Flows()
}

@Serializable
data class OAuthFlows (
        val authorizationCode: AuthorizationCodeOAuthFlow? = null,
        val clientCredentials: ClientCredentialsFlow? = null,
        val implicit: ImplicitOAuthFlow? = null,
        val password: PasswordOAuthFlow? = null
)

@Serializable
data class AuthorizationCodeOAuthFlow (
        @SerialName("authorizationUrl")
        val authorizationURL: String,

        @SerialName("refreshUrl")
        val refreshURL: String? = null,

        val scopes: Map<String, String>? = null,

        @SerialName("tokenUrl")
        val tokenURL: String
)

@Serializable
data class ClientCredentialsFlow (
        @SerialName("refreshUrl")
        val refreshURL: String? = null,

        val scopes: Map<String, String>? = null,

        @SerialName("tokenUrl")
        val tokenURL: String
)

@Serializable
data class ImplicitOAuthFlow (
        @SerialName("authorizationUrl")
        val authorizationURL: String,

        @SerialName("refreshUrl")
        val refreshURL: String? = null,

        val scopes: Map<String, String>
)

@Serializable
data class PasswordOAuthFlow (
        @SerialName("refreshUrl")
        val refreshURL: String? = null,

        val scopes: Map<String, String>? = null,

        @SerialName("tokenUrl")
        val tokenURL: String
)

@Serializable
data class Info (
        val contact: Contact? = null,
        val description: String? = null,
        val license: License? = null,
        val termsOfService: String? = null,
        val title: String,
        val version: String
)

@Serializable
data class Contact (
        val email: String? = null,
        val name: String? = null,
        val url: String? = null
)

@Serializable
data class License (
        val name: String,
        val url: String? = null
)

@Serializable
data class Tag (
        val description: String? = null,
        val externalDocs: ExternalDocumentation? = null,
        val name: String
)
