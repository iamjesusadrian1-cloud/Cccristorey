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
        "https://www.youtube.com/embed/live_stream?channel=$YOUTUBE_CHANNEL_ID&autoplay=0&rel=0"
    const val YOUTUBE_CHANNEL_URL =
        "https://www.youtube.com/$YOUTUBE_HANDLE"

    fun videoEmbedUrl(videoId: String): String =
        "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0"

    fun videoWatchUrl(videoId: String): String =
        "https://www.youtube.com/watch?v=$videoId"

    // Instagram oficial (Sin tokens de Meta en v1; listo para agregador RSS/Embed o futura migración Graph API)
    const val INSTAGRAM_HANDLE = "@cccristoreyve"
    const val INSTAGRAM_PROFILE_URL = "https://www.instagram.com/cccristoreyve"
    const val INSTAGRAM_EMBED_URL = "https://www.instagram.com/cccristoreyve/embed"

    /**
     * URL opcional de feed RSS gratuito generado con RSS.app para @cccristoreyve.
     * Ejemplo: "https://rss.app/feeds/XXXXXXXXXXXX.xml"
     * Si se deja vacío, la app muestra las publicaciones sincronizadas desde Firestore (webhook)
     * y el visor embebido oficial de @cccristoreyve.
     */
    const val SOCIAL_AGGREGATOR_RSS_URL = ""

    /**
     * URL opcional de widget embebible gratuito (SnapWidget / EmbedSocial).
     * Si se deja vacío, se usa INSTAGRAM_EMBED_URL.
     */
    const val SOCIAL_WIDGET_EMBED_URL = ""

    // Información institucional de Cristo Rey
    const val ORGANIZATION_SUBTITLE = "Centro Cristiano Cristo Rey"
    const val CONTACT_EMAIL = "contacto@cccristorey.org"
    const val WORSHIP_ADDRESS = "Sede Cristo Rey • Consulta nuestras redes para ubicaciones de campus"
}
