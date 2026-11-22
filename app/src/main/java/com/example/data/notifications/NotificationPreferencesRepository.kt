package com.example.data.notifications

import android.util.Log
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppStatusDoc
import com.example.data.model.UserNotificationToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotificationPreferencesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google antes de acceder a Firestore.")
        check(uid == expectedUserId) { "La sesión cambió; rechazando escritura entre usuarios." }
        return uid
    }

    fun observeTokenPreferences(userId: String): Flow<UserNotificationToken?> {
        val path = "tokens/$userId"
        return db.collection("tokens")
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserNotificationToken::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeLiveStatusDoc(userId: String): Flow<AppStatusDoc?> {
        val path = "app_status/live"
        return db.collection("app_status")
            .document("live")
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    AppStatusDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun fetchFcmTokenSafe(): String {
        return try {
            FirebaseMessaging.getInstance().token.await().orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "FCM token unavailable yet (configure FCM in Firebase Console if needed): ${e.message}")
            ""
        }
    }

    suspend fun syncFcmTokenAndEnsureDefaults(
        userId: String,
        fcmToken: String
    ): Result<Unit> {
        val path = "tokens/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeToken = fcmToken.take(512)
            val docRef = db.collection("tokens").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fcmToken" to safeToken,
                    "platform" to "android",
                    "liveEnabled" to true,
                    "videosEnabled" to true,
                    "socialEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else if (safeToken.isNotBlank() && existing.getString("fcmToken") != safeToken) {
                docRef.update(
                    mapOf(
                        "fcmToken" to safeToken,
                        "platform" to "android",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        liveEnabled: Boolean,
        videosEnabled: Boolean,
        socialEnabled: Boolean
    ): Result<Unit> {
        val path = "tokens/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("tokens").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val fcmToken = fetchFcmTokenSafe().take(512)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "fcmToken" to fcmToken,
                        "platform" to "android",
                        "liveEnabled" to liveEnabled,
                        "videosEnabled" to videosEnabled,
                        "socialEnabled" to socialEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "liveEnabled" to liveEnabled,
                        "videosEnabled" to videosEnabled,
                        "socialEnabled" to socialEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "NotifPrefsRepo"
    }
}
