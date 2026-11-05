package com.laioffer

import java.net.URI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class Album(
    val id: Int,
    @SerialName("album") val name: String,
    val year: String,
    val cover: String,
    val artists: String,
    val description: String,
)

@Serializable
data class Section(@SerialName("section_title") val title: String, val albums: List<Album>)

@Serializable
data class Playlist(val id: Int, val songs: List<Song>)

@Serializable
data class Song(val name: String, val lyric: String, val src: String, val length: String)

/** Validated once at startup; requests never reread or reinterpret the JSON files. */
class MusicCatalog private constructor(
    val feed: List<Section>,
    val playlists: List<Playlist>,
    val audioNames: Set<String>,
) {
    private val byId = playlists.associateBy { it.id }
    fun playlist(id: Int): Playlist? = byId[id]

    companion object {
        fun load(classLoader: ClassLoader = MusicCatalog::class.java.classLoader): MusicCatalog = load(
            readText = { name -> classLoader.getResourceAsStream(name)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } },
            audioExists = { name -> classLoader.getResource("static/songs/$name") != null },
        )

        internal fun load(readText: (String) -> String?, audioExists: (String) -> Boolean): MusicCatalog {
            val feedText = requireNotNull(readText("feed.json")) { "The music feed resource is missing." }
            val playlistText = requireNotNull(readText("playlists.json")) { "The playlist resource is missing." }
            val feed = Json.decodeFromString<List<Section>>(feedText)
            val playlists = Json.decodeFromString<List<Playlist>>(playlistText)
            require(feed.isNotEmpty() && playlists.isNotEmpty()) { "The music catalog is empty." }
            require(playlists.all { it.id > 0 && it.songs.isNotEmpty() }) { "Each playlist needs an ID and songs." }
            require(playlists.map { it.id }.distinct().size == playlists.size) { "Playlist IDs must be unique." }
            val playlistIds = playlists.map { it.id }.toSet()
            val albums = feed.flatMap { section ->
                require(section.title.isNotBlank() && section.albums.isNotEmpty()) { "Feed sections must contain albums." }
                section.albums
            }
            require(albums.all { it.id in playlistIds && it.name.isNotBlank() && it.artists.isNotBlank() }) {
                "Each album must reference an available playlist."
            }
            require(albums.groupBy { it.id }.all { (_, versions) -> versions.distinct().size == 1 }) {
                "Repeated album IDs must have consistent metadata."
            }
            val audioNames = playlists.flatMap { it.songs }.map { song ->
                require(song.name.isNotBlank() && Regex("\\d{1,3}:[0-5]\\d").matches(song.length)) {
                    "Song metadata is invalid."
                }
                val uri = runCatching { URI(song.src) }.getOrNull()
                val name = uri?.path?.removePrefix("/songs/")
                require(uri != null && !uri.isAbsolute && uri.rawAuthority == null && uri.query == null && uri.fragment == null &&
                    uri.path.startsWith("/songs/") && !name.isNullOrBlank() && '/' !in name && '\\' !in name &&
                    name != "." && name != ".." && name.endsWith(".mp3", ignoreCase = true) &&
                    name.none { it.isISOControl() }) { "Audio URLs must reference a local song resource." }
                require(audioExists(name)) { "A referenced audio resource is missing." }
                name
            }.toSet()
            return MusicCatalog(feed, playlists, audioNames)
        }
    }
}
