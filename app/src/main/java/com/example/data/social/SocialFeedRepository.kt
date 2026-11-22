package com.example.data.social

import android.util.Log
import android.util.Xml
import com.example.data.CristoReyConfig
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.SocialPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Abstracción modular para el feed de redes sociales (@cccristoreyve).
 * Permite iniciar sin tokens usando Firestore (webhook de agregador) o RSS.app,
 * y migrar limpiamente a Instagram Graph API en el futuro implementando InstagramGraphApiDataSource.
 */
interface SocialFeedDataSource {
    suspend fun fetchAggregatorFeed(): Result<List<SocialPost>>
}

class SocialFeedRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val httpClient: OkHttpClient = defaultClient()
) : SocialFeedDataSource {

    /**
     * Observa en tiempo real las publicaciones sincronizadas en Firestore (/social_posts)
     * con filtro estricto published == true alineado a firestore.rules.
     */
    fun observeFirestoreSocialPosts(): Flow<List<SocialPost>> {
        val uid = auth.currentUser?.uid
        val path = "social_posts"
        return db.collection(path)
            .whereEqualTo("published", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SocialPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && uid != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    /**
     * Si el usuario configura CristoReyConfig.SOCIAL_AGGREGATOR_RSS_URL (ej. RSS.app),
     * descarga y parsea las publicaciones reales de @cccristoreyve sin necesidad de Meta tokens.
     */
    override suspend fun fetchAggregatorFeed(): Result<List<SocialPost>> =
        withContext(Dispatchers.IO) {
            val rssUrl = CristoReyConfig.SOCIAL_AGGREGATOR_RSS_URL
            if (rssUrl.isBlank()) {
                return@withContext Result.success(emptyList())
            }
            try {
                val request = Request.Builder()
                    .url(rssUrl)
                    .header("User-Agent", "CristoReyAndroidApp/1.0")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Error al obtener feed social (HTTP ${response.code})")
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    Result.success(parseAggregatorRss(body))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching aggregator RSS feed", e)
                Result.failure(e)
            }
        }

    internal fun parseAggregatorRss(xml: String): List<SocialPost> {
        if (xml.isBlank()) return emptyList()
        val posts = mutableListOf<SocialPost>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }

        var insideItem = false
        var title = ""
        var description = ""
        var link = ""
        var pubDate = ""
        var imageUrl = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && posts.size < 20) {
            val tag = parser.name.orEmpty()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "item" -> {
                            insideItem = true
                            title = ""
                            description = ""
                            link = ""
                            pubDate = ""
                            imageUrl = ""
                        }
                        "title" -> if (insideItem) title = parser.nextText().trim()
                        "description" -> if (insideItem) description = parser.nextText().trim()
                        "link" -> if (insideItem) link = parser.nextText().trim()
                        "pubDate" -> if (insideItem) pubDate = parser.nextText().trim()
                        "media:content", "enclosure" -> if (insideItem) {
                            val url = parser.getAttributeValue(null, "url").orEmpty()
                            if (url.isNotBlank() && imageUrl.isBlank()) imageUrl = url
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tag == "item" && insideItem) {
                        insideItem = false
                        val extractedImg = imageUrl.ifBlank { extractFirstImageSrc(description) }
                        val cleanCaption = stripHtml(title.ifBlank { description })
                        if (cleanCaption.isNotBlank() || extractedImg.isNotBlank()) {
                            posts.add(
                                SocialPost(
                                    id = link.hashCode().toString(),
                                    caption = cleanCaption.ifBlank { "Publicación de @cccristoreyve" },
                                    imageUrl = extractedImg,
                                    postUrl = link.ifBlank { CristoReyConfig.INSTAGRAM_PROFILE_URL },
                                    published = true,
                                    publishedAt = pubDate.take(16)
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return posts
    }

    private fun extractFirstImageSrc(html: String): String {
        val regex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun stripHtml(raw: String): String {
        return raw.replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val TAG = "SocialFeedRepository"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

/**
 * Adaptador preparado para migración futura a Instagram Graph API oficial
 * cuando se disponga de un token de Meta (IG User ID + Access Token).
 * Ver README.md sección "Migración futura a Instagram Graph API".
 */
@Suppress("unused")
class InstagramGraphApiDataSource(
    private val igUserId: String,
    private val accessToken: String
) : SocialFeedDataSource {
    override suspend fun fetchAggregatorFeed(): Result<List<SocialPost>> {
        // Endpoint oficial futuro:
        // GET https://graph.facebook.com/v20.0/$igUserId/media?fields=id,caption,media_url,permalink,timestamp&access_token=$accessToken
        return Result.failure(
            NotImplementedError("Configurar IG_USER_ID y META_ACCESS_TOKEN en Secrets para activar Graph API.")
        )
    }
}
