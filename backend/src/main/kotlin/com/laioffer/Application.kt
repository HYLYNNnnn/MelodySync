package com.laioffer

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.resolveResource
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class HealthResponse(val status: String = "ok", val service: String = "MelodySync")

data class ServerConfig(val host: String, val port: Int) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): ServerConfig {
            val host = environment["HOST"] ?: "127.0.0.1"
            val port = (environment["PORT"] ?: "8080").toIntOrNull()
            require(host.isNotBlank() && host.none { it.isWhitespace() || it.isISOControl() }) { "HOST must be a valid bind address." }
            require(port != null && port in 1..65535) { "PORT must be between 1 and 65535." }
            return ServerConfig(host, port)
        }
    }
}

fun main() {
    val config = ServerConfig.fromEnvironment()
    embeddedServer(Netty, host = config.host, port = config.port) { module() }.start(wait = true)
}

fun Application.module(catalog: MusicCatalog = MusicCatalog.load()) {
    install(ContentNegotiation) {
        json(Json { prettyPrint = false; encodeDefaults = true })
    }
    install(AutoHeadResponse)
    install(PartialContent) { maxRangeCount = 4 }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            this@module.log.error("Request failed", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("INTERNAL_ERROR", "The music service is unavailable. Please try again."))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiError("NOT_FOUND", "The requested music resource was not found."))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(status, ApiError("METHOD_NOT_ALLOWED", "This request method is not supported."))
        }
    }
    routing {
        get("/") { call.respond(HealthResponse()) }
        get("/health") { call.respond(HealthResponse()) }
        get("/feed") { call.respond(catalog.feed) }
        get("/playlists") { call.respond(catalog.playlists) }
        get("/playlist") {
            call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_PLAYLIST_ID", "Provide a positive playlist ID."))
        }
        get("/playlist/") {
            call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_PLAYLIST_ID", "Provide a positive playlist ID."))
        }
        get("/playlist/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()?.takeIf { it > 0 }
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_PLAYLIST_ID", "Provide a positive playlist ID."))
            } else {
                val playlist = catalog.playlist(id)
                if (playlist == null) call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "The requested playlist was not found."))
                else call.respond(playlist)
            }
        }
        // Serve only songs referenced by the validated catalog. Other classpath files stay private.
        get("/songs/{name}") {
            val name = call.parameters["name"]
            if (name == null || name !in catalog.audioNames) {
                call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "The requested song was not found."))
            } else {
                call.response.header("X-Content-Type-Options", "nosniff")
                val resource = call.resolveResource("static/songs/$name")
                if (resource == null) {
                    call.respond(HttpStatusCode.NotFound, ApiError("NOT_FOUND", "The requested song was not found."))
                } else {
                    call.respond(resource)
                }
            }
        }
    }
}
