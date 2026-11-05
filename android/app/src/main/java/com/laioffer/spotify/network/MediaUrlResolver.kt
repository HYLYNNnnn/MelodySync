package com.laioffer.spotify.network

import java.net.URI
import javax.inject.Inject
import javax.inject.Named

/** Accept server-relative URLs and preserve already-escaped UTF-8 path segments. */
class MediaUrlResolver @Inject constructor(@Named("apiBaseUrl") baseUrl: String) {
    private val base = parse(baseUrl).also {
        require(it.isAbsolute && it.host != null && it.rawUserInfo == null && it.rawFragment == null
            && it.scheme.lowercase() in setOf("http", "https")) { "Use an HTTP(S) API base URL." }
    }

    fun resolve(source: String): String {
        val supplied = parse(source)
        require(supplied.rawUserInfo == null && supplied.rawFragment == null) { "Unsupported media URL." }
        require(supplied.isAbsolute || supplied.rawAuthority == null) { "Use an absolute or server-relative media URL." }
        val target = if (supplied.isAbsolute && supplied.host == "10.0.2.2") {
            // Old course fixtures used the emulator host in every audio URL.
            base.resolve(URI(supplied.rawPath + (supplied.rawQuery?.let { "?$it" } ?: "")))
        } else base.resolve(supplied)
        require(target.scheme?.lowercase() in setOf("http", "https") && target.host != null
            && target.rawUserInfo == null && target.rawFragment == null) { "Unsupported media URL." }
        return target.normalize().toASCIIString()
    }

    private fun parse(value: String): URI {
        require(value.isNotBlank() && value.none { it.code < 32 || it.code == 127 }) { "Missing or invalid media URL." }
        // URI preserves existing %XX escapes, unlike rebuilding an encoded path component.
        return URI(value.trim().replace(" ", "%20"))
    }
}
