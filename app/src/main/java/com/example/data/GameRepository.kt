package com.example.data

import android.content.Context
import com.example.R
import com.example.game.ControlMode
import com.example.game.FlipSkin
import com.example.game.RoomTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("Signed-in user changed; refusing cross-user Firestore access.")
        }
        return uid
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentRuns(userId: String, limit: Long = 20L): Flow<List<FlipRun>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/runs"
        emitAll(
            db.collection("users").document(uid).collection("runs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FlipRun::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        localSettings: StoredGameSettings
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val cleanName = sanitizeDisplayName(displayName)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "bestScore" to localSettings.bestScore.toLong().coerceIn(0L, MAX_SCORE),
                    "bestStreak" to localSettings.bestStreak.toLong().coerceIn(0L, MAX_COUNT),
                    "totalRuns" to 0L,
                    "totalPerfects" to 0L,
                    "selectedSkinId" to localSettings.selectedSkin.id.take(MAX_ID_LEN),
                    "selectedThemeId" to localSettings.selectedTheme.id.take(MAX_ID_LEN),
                    "controlModeId" to localSettings.controlMode.id.take(MAX_ID_LEN),
                    "soundEnabled" to localSettings.soundEnabled,
                    "hapticsEnabled" to localSettings.hapticsEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncPreferences(
        userId: String,
        displayName: String,
        skin: FlipSkin,
        theme: RoomTheme,
        mode: ControlMode,
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        fallbackSettings: StoredGameSettings
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val cleanName = sanitizeDisplayName(displayName)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "selectedSkinId" to skin.id.take(MAX_ID_LEN),
                        "selectedThemeId" to theme.id.take(MAX_ID_LEN),
                        "controlModeId" to mode.id.take(MAX_ID_LEN),
                        "soundEnabled" to soundEnabled,
                        "hapticsEnabled" to hapticsEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ensureUserProfile(uid, cleanName, fallbackSettings).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBestStats(
        userId: String,
        displayName: String,
        score: Int,
        streak: Int,
        fallbackSettings: StoredGameSettings
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val clampedScore = score.toLong().coerceIn(0L, MAX_SCORE)
            val clampedStreak = streak.toLong().coerceIn(0L, MAX_COUNT)
            val cleanName = sanitizeDisplayName(displayName)

            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) {
                    val initPayload = mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "bestScore" to maxOf(clampedScore, fallbackSettings.bestScore.toLong()).coerceIn(0L, MAX_SCORE),
                        "bestStreak" to maxOf(clampedStreak, fallbackSettings.bestStreak.toLong()).coerceIn(0L, MAX_COUNT),
                        "totalRuns" to 0L,
                        "totalPerfects" to 0L,
                        "selectedSkinId" to fallbackSettings.selectedSkin.id.take(MAX_ID_LEN),
                        "selectedThemeId" to fallbackSettings.selectedTheme.id.take(MAX_ID_LEN),
                        "controlModeId" to fallbackSettings.controlMode.id.take(MAX_ID_LEN),
                        "soundEnabled" to fallbackSettings.soundEnabled,
                        "hapticsEnabled" to fallbackSettings.hapticsEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(docRef, initPayload)
                } else {
                    val prevBestScore = (snap.getLong("bestScore") ?: 0L).coerceIn(0L, MAX_SCORE)
                    val prevBestStreak = (snap.getLong("bestStreak") ?: 0L).coerceIn(0L, MAX_COUNT)
                    val prevRuns = (snap.getLong("totalRuns") ?: 0L).coerceIn(0L, MAX_SCORE)
                    val prevPerfects = (snap.getLong("totalPerfects") ?: 0L).coerceIn(0L, MAX_SCORE)
                    val nextBestScore = maxOf(prevBestScore, clampedScore)
                    val nextBestStreak = maxOf(prevBestStreak, clampedStreak)
                    if (nextBestScore > prevBestScore || nextBestStreak > prevBestStreak) {
                        transaction.update(
                            docRef,
                            mapOf(
                                "bestScore" to nextBestScore,
                                "bestStreak" to nextBestStreak,
                                "totalRuns" to prevRuns,
                                "totalPerfects" to prevPerfects,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedRun(
        userId: String,
        displayName: String,
        score: Int,
        maxStreak: Int,
        perfectCount: Int,
        jumpsCompleted: Int,
        skin: FlipSkin,
        theme: RoomTheme,
        controlMode: ControlMode,
        failReason: String,
        fallbackSettings: StoredGameSettings
    ): Result<Unit> {
        val runRef = db.collection("users").document(userId).collection("runs").document()
        val path = runRef.path
        return try {
            val uid = requireUserId(userId)
            val userRef = db.collection("users").document(uid)
            val cleanName = sanitizeDisplayName(displayName)
            val clampedScore = score.toLong().coerceIn(0L, MAX_SCORE)
            val clampedStreak = maxStreak.toLong().coerceIn(0L, MAX_COUNT)
            val clampedPerfects = perfectCount.toLong().coerceIn(0L, MAX_COUNT)
            val clampedJumps = jumpsCompleted.toLong().coerceIn(0L, MAX_COUNT)
            val cleanFailReason = failReason.trim().take(120).ifEmpty { null }

            val runPayload = mapOf(
                "userId" to uid,
                "score" to clampedScore,
                "maxStreak" to clampedStreak,
                "perfectCount" to clampedPerfects,
                "jumpsCompleted" to clampedJumps,
                "skinId" to skin.id.take(MAX_ID_LEN),
                "themeId" to theme.id.take(MAX_ID_LEN),
                "controlModeId" to controlMode.id.take(MAX_ID_LEN),
                "failReason" to cleanFailReason,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                if (!userSnap.exists()) {
                    val initProfile = mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "bestScore" to maxOf(clampedScore, fallbackSettings.bestScore.toLong()).coerceIn(0L, MAX_SCORE),
                        "bestStreak" to maxOf(clampedStreak, fallbackSettings.bestStreak.toLong()).coerceIn(0L, MAX_COUNT),
                        "totalRuns" to 1L,
                        "totalPerfects" to clampedPerfects,
                        "selectedSkinId" to skin.id.take(MAX_ID_LEN),
                        "selectedThemeId" to theme.id.take(MAX_ID_LEN),
                        "controlModeId" to controlMode.id.take(MAX_ID_LEN),
                        "soundEnabled" to fallbackSettings.soundEnabled,
                        "hapticsEnabled" to fallbackSettings.hapticsEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(userRef, initProfile)
                } else {
                    val prevBestScore = (userSnap.getLong("bestScore") ?: 0L).coerceIn(0L, MAX_SCORE)
                    val prevBestStreak = (userSnap.getLong("bestStreak") ?: 0L).coerceIn(0L, MAX_COUNT)
                    val prevRuns = (userSnap.getLong("totalRuns") ?: 0L).coerceIn(0L, MAX_SCORE)
                    val prevPerfects = (userSnap.getLong("totalPerfects") ?: 0L).coerceIn(0L, MAX_SCORE)
                    transaction.update(
                        userRef,
                        mapOf(
                            "bestScore" to maxOf(prevBestScore, clampedScore),
                            "bestStreak" to maxOf(prevBestStreak, clampedStreak),
                            "totalRuns" to (prevRuns + 1L).coerceAtMost(MAX_SCORE),
                            "totalPerfects" to (prevPerfects + clampedPerfects).coerceAtMost(MAX_SCORE),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                transaction.set(runRef, runPayload)
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRun(userId: String, runId: String): Result<Unit> {
        val path = "users/$userId/runs/$runId"
        return try {
            val uid = requireUserId(userId)
            require(runId.isNotBlank() && ID_REGEX.matches(runId)) { "Invalid run ID" }
            db.collection("users").document(uid).collection("runs").document(runId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun sanitizeDisplayName(name: String): String {
        return name.trim().take(80).ifBlank { "Flipper" }
    }

    private companion object {
        const val MAX_SCORE = 1_000_000L
        const val MAX_COUNT = 10_000L
        const val MAX_ID_LEN = 40
        val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
