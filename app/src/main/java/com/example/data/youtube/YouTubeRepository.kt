package com.example.data.youtube

import android.util.Log
import android.util.Xml
import com.example.data.CristoReyConfig
import com.example.data.model.YouTubeVideo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class YouTubeRepository(
    private val client: OkHttpClient = defaultClient()
) {
    /**
     * Obtiene y parsea el feed RSS público de YouTube (máximo 15 videos) SIN API Key.
     * URL: https://www.youtube.com/feeds/videos.xml?channel_id=UCfg1whOZLgGrtneGLJGCHqQ
     */
    suspend fun fetchRecentVideos(limit: Int = 15): Result<List<YouTubeVideo>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(CristoReyConfig.YOUTUBE_RSS_URL)
                    .header("User-Agent", "CristoReyAndroidApp/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Error al obtener videos (HTTP ${response.code})")
                        )
                    }
                    val xmlBody = response.body?.string().orEmpty()
                    if (xmlBody.isBlank()) {
                        return@withContext Result.success(emptyList())
                    }
                    val videos = parseYouTubeAtomFeed(xmlBody, limit)
                    Result.success(videos)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching YouTube RSS feed", e)
                Result.failure(e)
            }
        }

    /**
     * Verifica directamente en la página pública /live del canal si existe "isLive":true SIN API Key.
     * Complementa el estado sincronizado por la Cloud Function en Firestore (app_status/live).
     */
    suspend fun checkLiveStatusDirect(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CristoReyConfig.YOUTUBE_LIVE_CHECK_URL)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Error al verificar estado en vivo (HTTP ${response.code})")
                    )
                }
                val html = response.body?.string().orEmpty()
                val isLive = html.contains("\"isLive\":true") ||
                    html.contains("\"isLiveNow\":true")
                Result.success(isLive)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error checking direct live status", e)
            Result.failure(e)
        }
    }

    internal fun parseYouTubeAtomFeed(xml: String, limit: Int = 15): List<YouTubeVideo> {
        val result = mutableListOf<YouTubeVideo>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }

        var insideEntry = false
        var videoId = ""
        var title = ""
        var published = ""
        var thumbnailUrl = ""
        var linkHref = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && result.size < limit) {
            val tagName = parser.name.orEmpty()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "entry" -> {
                            insideEntry = true
                            videoId = ""
                            title = ""
                            published = ""
                            thumbnailUrl = ""
                            linkHref = ""
                        }
                        "yt:videoId" -> if (insideEntry) {
                            videoId = parser.nextText().trim()
                        }
                        "title" -> if (insideEntry) {
                            title = parser.nextText().trim()
                        }
                        "published" -> if (insideEntry) {
                            published = parser.nextText().trim()
                        }
                        "link" -> if (insideEntry) {
                            val rel = parser.getAttributeValue(null, "rel")
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            if (rel == "alternate" || linkHref.isBlank()) {
                                linkHref = href
                            }
                        }
                        "media:thumbnail" -> if (insideEntry) {
                            val url = parser.getAttributeValue(null, "url").orEmpty()
                            if (url.isNotBlank()) thumbnailUrl = url
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "entry" && insideEntry) {
                        insideEntry = false
                        if (videoId.isNotBlank() && title.isNotBlank()) {
                            val resolvedThumb = thumbnailUrl.ifBlank {
                                "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                            }
                            val resolvedWatchUrl = linkHref.ifBlank {
                                CristoReyConfig.videoWatchUrl(videoId)
                            }
                            result.add(
                                YouTubeVideo(
                                    videoId = videoId,
                                    title = title,
                                    publishedAtRaw = published,
                                    formattedDate = formatIsoDateSpanish(published),
                                    thumbnailUrl = resolvedThumb,
                                    watchUrl = resolvedWatchUrl,
                                    embedUrl = CristoReyConfig.videoEmbedUrl(videoId)
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return result
    }

    private fun formatIsoDateSpanish(isoString: String): String {
        if (isoString.isBlank()) return "Reciente"
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )
        val spanishLocale = Locale("es", "ES")
        for (pattern in patterns) {
            try {
                val inputFormat = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = inputFormat.parse(isoString)
                if (parsed != null) {
                    val outputFormat = SimpleDateFormat("d MMM yyyy", spanishLocale)
                    return outputFormat.format(parsed)
                }
            } catch (_: Exception) {
                // Intentar siguiente patrón
            }
        }
        return isoString.take(10)
    }

    companion object {
        private const val TAG = "YouTubeRepository"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
