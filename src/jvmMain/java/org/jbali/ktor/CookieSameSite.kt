package org.jbali.ktor

import io.ktor.sessions.*
import kotlin.collections.set

/**
 * The SameSite attribute lets servers require that a cookie shouldn't be sent with
 * cross-origin requests (where Site is defined by the registrable domain), which
 * provides some protection against cross-site request forgery attacks (CSRF).
 */
enum class CookieSameSite {

    /**
     * The cookie is sent only to the same site as the one that originated it.
     * A request to site B initiated by clicking a link on site A will NOT include B's cookie.
     */
    Strict,

    /**
     * The cookie is sent only to the same site as the one that originated it,
     * with an exception for when the user navigates to a URL from an external site,
     * such as by following a link.
     *
     * This is the default value in modern browsers and has always been the default behaviour of cookies.
     */
    Lax,

    /**
     * Intention: the cookie has no restrictions on cross-site requests,
     * but starting in 2024, will start to be blocked from different domains anyway,
     * unless the cookie is Partitioned: https://developers.google.com/privacy-sandbox/3pcd/chips
     */
    None;

    companion object {
        const val attributeName = "SameSite"
    }

}

var CookieConfiguration.sameSite: CookieSameSite?
    get() =
        extensions[CookieSameSite.attributeName]?.let {
            CookieSameSite.valueOf(it)
        }
    set(v) {
        when (v) {
            null -> extensions.remove(CookieSameSite.attributeName)
            else -> extensions[CookieSameSite.attributeName] = v.name
        }
    }



fun CookieConfiguration.hasExtensionFlag(name: String): Boolean = name in extensions
fun CookieConfiguration.setExtensionFlag(name: String, present: Boolean) {
    when (present) {
        true -> extensions[name] = null
        false -> extensions.remove(name)
    }
}

var CookieConfiguration.partitioned: Boolean
    get() = hasExtensionFlag("Partitioned")
    set(v) = setExtensionFlag("Partitioned", v)
