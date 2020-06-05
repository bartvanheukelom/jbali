package org.jbali.util

import java.io.Serializable
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlValue

/**
 * Can be used to store a password or other secret, prevents accidentally
 * printing/logging them or passing them where a non-secret string is accepted.
 *
 * Contains no countermeasures against runtime (memory) hacks or anything like that!
 */
@XmlAccessorType(XmlAccessType.FIELD)
data class Password(@field:XmlValue private val value: String) : Serializable {

    // no-arg constructor for JAXB metadata, unmarshalling is not supported!
    @Suppress("unused") private constructor() : this(fakeConstructorValue())

    /**
     * Returns the value.
     * The name of this getter is intentionally verbose.
     */
    fun accessPasswordValue(): String = value

    override fun toString() = "Password(*****)"

    companion object {
        const val serialVersionUID = 1L
    }

}