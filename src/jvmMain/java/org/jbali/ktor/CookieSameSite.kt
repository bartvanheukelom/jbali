package org.jbali.ktor

import io.ktor.sessions.CookieConfiguration

/**
 * The SameSite attribute lets servers require that a cookie shouldn't be sent with
 * cross-origin requests (where Site is defined by the registrable domain), which
 * provides some protection against cross-site request forgery attacks (CSRF).
 */
enum class CookieSameSite {

    /**
     * The cookie is sent only to the same site as the one that originated it.
     */
    Strict,

    /**
     * The cookie is sent only to the same site as the one that originated it,
     * with an exception for when the user navigates to a URL from an external site,
     * such as by following a link.
     */
    Lax,

    /**
     * The cookie has no restrictions on cross-site requests.
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
