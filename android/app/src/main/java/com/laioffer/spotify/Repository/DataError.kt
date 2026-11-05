package com.laioffer.spotify.repository

import java.io.IOException
import retrofit2.Response

class MusicDataException(message: String, cause: Throwable? = null) : IOException(message, cause)

internal fun <T> Response<T>.requireBody(label: String): T {
    if (!isSuccessful) {
        errorBody()?.close()
        val message = when (code()) {
            404 -> "The requested $label was not found."
            429 -> "The music server is busy. Please try again shortly."
            in 500..599 -> "The music server is unavailable. Please try again."
            else -> "Could not load $label (HTTP ${code()})."
        }
        throw MusicDataException(message)
    }
    return body() ?: throw MusicDataException("The music server returned an empty $label response.")
}

internal fun readableDataError(error: Exception, fallback: String): String = when (error) {
    is MusicDataException -> error.message ?: fallback
    is IOException -> "Cannot reach the music server. Check your connection and retry."
    else -> fallback
}
