package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.youtube.YouTubeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cristo Rey", appName)
    }

    @Test
    fun `parseYouTubeAtomFeed extracts video id title thumbnail and links`() {
        val sampleAtomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015"
                  xmlns:media="http://search.yahoo.com/mrss/"
                  xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <yt:videoId>abc123XYZ</yt:videoId>
                <title>Culto Dominical — Fe y Esperanza</title>
                <link rel="alternate" href="https://www.youtube.com/watch?v=abc123XYZ"/>
                <published>2026-11-15T15:00:00+00:00</published>
                <media:group>
                  <media:thumbnail url="https://i2.ytimg.com/vi/abc123XYZ/hqdefault.jpg" width="480" height="360"/>
                </media:group>
              </entry>
            </feed>
        """.trimIndent()

        val repo = YouTubeRepository()
        val parsed = repo.parseYouTubeAtomFeed(sampleAtomXml)

        assertEquals(1, parsed.size)
        val video = parsed.first()
        assertEquals("abc123XYZ", video.videoId)
        assertEquals("Culto Dominical — Fe y Esperanza", video.title)
        assertEquals("https://i2.ytimg.com/vi/abc123XYZ/hqdefault.jpg", video.thumbnailUrl)
        assertEquals("https://www.youtube.com/watch?v=abc123XYZ", video.watchUrl)
        assertTrue(video.embedUrl.contains("https://www.youtube.com/embed/abc123XYZ"))
    }
}
