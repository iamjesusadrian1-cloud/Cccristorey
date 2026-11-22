package com.example.ui.components

import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebViewClient personalizado para embeds de YouTube.
 * Sobrescribe `shouldOverrideUrlLoading` para garantizar que cualquier navegación o redirección
 * del reproductor embebido conserve los headers `Referer: https://com.mi.app` y
 * `Referrer-Policy: strict-origin-when-cross-origin`, evitando los errores 152-4 y 153-4.
 *
 * Nota: No intercepta el HTML en `shouldInterceptRequest` con OkHttp porque reemplazar la respuesta
 * nativa elimina `document.referrer` y headers internos de Chromium (`Sec-Fetch-*`), lo cual
 * dispara el error 153-4 de configuración del reproductor de YouTube.
 */
class YouTubeEmbedWebViewClient(
    private val onPageFinishedCallback: (WebView?, String?) -> Unit = { _, _ -> },
    private val onMainFrameErrorCallback: (Int?) -> Unit = {},
    private val onOpenExternal: ((String) -> Unit)? = null,
    private val onRenderProcessGoneCallback: (WebView?, RenderProcessGoneDetail?) -> Boolean = { _, _ -> false }
) : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedCallback(view, url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode else null
            onMainFrameErrorCallback(code)
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val target = request?.url?.toString().orEmpty()
        if (target.isBlank()) return false

        // Enlaces externos de YouTube (Watch / Shorts / Canal) se abren en la app/navegador externo
        if ((target.startsWith("https://www.youtube.com/watch") ||
                target.startsWith("https://m.youtube.com/watch") ||
                target.startsWith("https://youtu.be/")) && onOpenExternal != null
        ) {
            onOpenExternal.invoke(target)
            return true
        }

        // Asegurar que redirecciones o recargas del marco principal hacia /embed/ mantengan el Referer de app
        if (request?.isForMainFrame == true && isYouTubeEmbedUrl(target)) {
            val existingReferer = request.requestHeaders.entries
                .firstOrNull { it.key.equals("Referer", ignoreCase = true) }
                ?.value
            if (existingReferer?.startsWith(AUTHORIZED_REFERER) != true && view != null) {
                view.loadUrl(target, AUTHORIZED_HEADERS)
                return true
            }
        }

        return false
    }

    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean = onRenderProcessGoneCallback(view, detail)

    private fun isYouTubeEmbedUrl(url: String): Boolean =
        url.startsWith("https://www.youtube.com/embed/") ||
            url.startsWith("https://www.youtube-nocookie.com/embed/")

    companion object {
        const val AUTHORIZED_REFERER = "https://com.mi.app"
        const val REFERRER_POLICY = "strict-origin-when-cross-origin"

        val AUTHORIZED_HEADERS: Map<String, String> = mapOf(
            "Referer" to AUTHORIZED_REFERER,
            "Referrer-Policy" to REFERRER_POLICY
        )
    }
}
