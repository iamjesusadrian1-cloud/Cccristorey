package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CristoReyConfig
import com.example.data.model.AppStatusDoc
import com.example.data.model.LiveStreamStatus
import com.example.data.model.UserNotificationToken
import com.example.data.model.YouTubeVideo
import com.example.data.notifications.NotificationPreferencesRepository
import com.example.data.youtube.YouTubeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class CristoReyViewModel(
    private val currentUserId: String,
    private val youTubeRepository: YouTubeRepository,
    private val notificationRepository: NotificationPreferencesRepository
) : ViewModel() {

    private val _videosState = MutableStateFlow<UiState<List<YouTubeVideo>>>(UiState.Loading)
    val videosState: StateFlow<UiState<List<YouTubeVideo>>> = _videosState.asStateFlow()

    private val _directLiveCheck = MutableStateFlow<Boolean?>(null)
    private val _isCheckingLive = MutableStateFlow(false)
    val isCheckingLive: StateFlow<Boolean> = _isCheckingLive.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val notificationPrefsState: StateFlow<UiState<UserNotificationToken?>> =
        notificationRepository.observeTokenPreferences(currentUserId)
            .map<UserNotificationToken?, UiState<UserNotificationToken?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing notification preferences", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar las preferencias."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val firestoreLiveDoc: StateFlow<AppStatusDoc?> =
        notificationRepository.observeLiveStatusDoc(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Non-fatal error observing app_status/live", firestoreError ?: error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val liveStreamStatus: StateFlow<LiveStreamStatus> =
        combine(firestoreLiveDoc, _directLiveCheck) { doc, direct ->
            val isLive = (doc?.liveActive == true) || (direct == true)
            LiveStreamStatus(
                liveActive = isLive,
                title = doc?.title?.takeIf { it.isNotBlank() }
                    ?: if (isLive) "Transmisión en Vivo — Cristo Rey" else "Próxima Transmisión — Cristo Rey",
                source = if (doc != null) "Firestore + YouTube" else "YouTube",
                embedUrl = CristoReyConfig.YOUTUBE_LIVE_EMBED_URL,
                watchUrl = CristoReyConfig.YOUTUBE_LIVE_WATCH_URL
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LiveStreamStatus(
                liveActive = false,
                embedUrl = CristoReyConfig.YOUTUBE_LIVE_EMBED_URL,
                watchUrl = CristoReyConfig.YOUTUBE_LIVE_WATCH_URL
            )
        )

    init {
        refreshVideos()
        refreshLiveStatus()
        syncFcmTokenOnStart()
    }

    fun refreshVideos() {
        viewModelScope.launch {
            if (_videosState.value !is UiState.Success) {
                _videosState.value = UiState.Loading
            }
            youTubeRepository.fetchRecentVideos(limit = 15)
                .onSuccess { videos ->
                    _videosState.value = UiState.Success(videos)
                }
                .onFailure { err ->
                    if (_videosState.value !is UiState.Success) {
                        _videosState.value = UiState.Error(
                            err.localizedMessage ?: "No se pudieron cargar los videos de YouTube."
                        )
                    } else {
                        _statusMessage.value = "No se pudo actualizar la lista de videos."
                    }
                }
        }
    }

    fun refreshLiveStatus() {
        viewModelScope.launch {
            _isCheckingLive.value = true
            youTubeRepository.checkLiveStatusDirect()
                .onSuccess { isLive ->
                    _directLiveCheck.value = isLive
                }
                .onFailure {
                    Log.w(TAG, "Direct live check failed, relying on Firestore/player")
                }
            _isCheckingLive.value = false
        }
    }

    fun syncFcmTokenOnStart() {
        viewModelScope.launch {
            val token = notificationRepository.fetchFcmTokenSafe()
            notificationRepository.syncFcmTokenAndEnsureDefaults(currentUserId, token)
                .onFailure { err ->
                    Log.w(TAG, "Initial FCM sync failed: ${err.message}")
                }
        }
    }

    fun updateNotificationToggles(
        liveEnabled: Boolean,
        videosEnabled: Boolean
    ) {
        viewModelScope.launch {
            notificationRepository.updateNotificationPreferences(
                userId = currentUserId,
                liveEnabled = liveEnabled,
                videosEnabled = videosEnabled,
                socialEnabled = false
            ).onSuccess {
                _statusMessage.value = "Preferencias de notificación guardadas."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Error al guardar preferencias."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    companion object {
        private const val TAG = "CristoReyVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
