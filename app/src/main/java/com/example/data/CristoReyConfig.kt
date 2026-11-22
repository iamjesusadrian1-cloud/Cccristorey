package com.example.data

/**
 * Configuración centralizada de Cristo Rey.
 * Todos los identificadores oficiales ya están verificados y no requieren API Keys de YouTube ni Meta.
 */
object CristoReyConfig {
    const val APP_NAME = "Cristo Rey"

    // YouTube oficial (Sin API Key: RSS público + iFrame oficial + detección HTML del live)
    const val YOUTUBE_HANDLE = "@cccristorey"
    const val YOUTUBE_CHANNEL_ID = "UCfg1whOZLgGrtneGLJGCHqQ"
    const val YOUTUBE_RSS_URL =
        "https://www.youtube.com/feeds/videos.xml?channel_id=$YOUTUBE_CHANNEL_ID"
    const val YOUTUBE_LIVE_CHECK_URL =
        "https://www.youtube.com/channel/$YOUTUBE_CHANNEL_ID/live"
    const val YOUTUBE_LIVE_WATCH_URL =
        "https://www.youtube.com/$YOUTUBE_HANDLE/live"
    const val YOUTUBE_LIVE_EMBED_URL =
        "https://www.youtube.com/embed/live_stream?channel=$YOUTUBE_CHANNEL_ID&autoplay=0&playsinline=1&rel=0"
    const val YOUTUBE_CHANNEL_URL =
        "https://www.youtube.com/$YOUTUBE_HANDLE"

    fun videoEmbedUrl(videoId: String): String =
        "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"

    fun videoWatchUrl(videoId: String): String =
        "https://www.youtube.com/watch?v=$videoId"

    // Información institucional de Cristo Rey
    const val ORGANIZATION_SUBTITLE = "Centro Cristiano Cristo Rey"
    const val CONTACT_EMAIL = "contacto@cccristorey.org"
    const val WORSHIP_ADDRESS = "Sede Cristo Rey • Consulta nuestras redes para ubicaciones de campus"
}
