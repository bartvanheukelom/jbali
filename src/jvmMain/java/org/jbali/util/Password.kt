package org.jbali.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.jbali.kotser.singlePropertySerializer
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlValue
import java.io.Serializable as JavaSerializable

/**
 * Can be used to store a password or other secret, prevents accidentally
 * printing/logging them or passing them where a non-secret string is accepted.
 *
 * Contains no countermeasures against runtime (memory) hacks or anything like that!
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Serializable(with = Password.Companion::class)
data class Password(
    @field:XmlValue private val value: String
) : JavaSerializable {

    // no-arg constructor for JAXB metadata, unmarshalling is not supported!
    @Suppress("unused") private constructor() : this(fakeConstructorValue())

    /**
     * Returns the value.
     * The name of this getter is intentionally verbose.
     */
    fun accessPasswordValue(): String = value

    override fun toString() = "Password(*****)"

    companion object :
        KSerializer<Password> by singlePropertySerializer(prop = Password::value, wrap = ::Password)
    {
        const val serialVersionUID = 1L
    }

}