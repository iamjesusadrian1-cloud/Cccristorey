package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val publishedAtRaw: String,
    val formattedDate: String,
    val thumbnailUrl: String,
    val watchUrl: String,
    val embedUrl: String
)

data class LiveStreamStatus(
    val liveActive: Boolean = false,
    val title: String = "Transmisión en Vivo — Cristo Rey",
    val source: String = "YouTube",
    val embedUrl: String,
    val watchUrl: String
)

@Keep
@IgnoreExtraProperties
data class UserNotificationToken(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val platform: String = "android",
    val liveEnabled: Boolean = true,
    val videosEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val postUrl: String = "",
    val published: Boolean = true,
    val publishedAt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppStatusDoc(
    @DocumentId val id: String = "",
    val liveActive: Boolean = false,
    val title: String = "",
    val videoId: String = "",
    val link: String = "",
    val thumbnailUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
