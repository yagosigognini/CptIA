package br.com.CapitularIA.services

import br.com.CapitularIA.data.UserActionType
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GamificationService(
    private val db: FirebaseFirestore,
    private val achievementService: AchievementService = AchievementService(db)
) {
    private val tag = "GamificationService"
    private val socialXpDailyCap = 50L
    private val socialXpDayField = "socialXpDayUtc"
    private val socialXpTodayField = "socialXpToday"

    private val xpByAction = mapOf(
        UserActionType.RATE_BOOK to 10L,
        UserActionType.MARK_BOOK_AS_FINISHED to 50L,
        UserActionType.SEND_GROUP_MESSAGE to 5L,
        UserActionType.READING_CHECKIN to 20L
    )

    suspend fun processAction(
        userId: String,
        actionType: UserActionType,
        metadata: Map<String, Any?> = emptyMap(),
        idempotencyKey: String
    ): Boolean {
        val userRef = db.collection("users").document(userId)
        val actionRef = db.collection("user_actions").document(idempotencyKey)

        val applied = db.runTransaction { transaction ->
            val actionSnapshot = transaction.get(actionRef)
            if (actionSnapshot.exists()) {
                return@runTransaction false
            }

            val userSnapshot = transaction.get(userRef)
            if (!userSnapshot.exists()) {
                transaction.set(
                    userRef,
                    mapOf(
                        "totalXp" to 0L,
                        "finishedBooksCount" to 0L,
                        "ratedBooksCount" to 0L,
                        "groupMessageCount" to 0L,
                        "readingCheckinCount" to 0L,
                        "currentStreak" to 0L
                    ),
                    SetOptions.merge()
                )
            }
            val currentXp = userSnapshot.getLong("totalXp") ?: 0L
            val finishedCount = userSnapshot.getLong("finishedBooksCount") ?: 0L
            val ratedCount = userSnapshot.getLong("ratedBooksCount") ?: 0L
            val messageCount = userSnapshot.getLong("groupMessageCount") ?: 0L
            val checkinCount = userSnapshot.getLong("readingCheckinCount") ?: 0L

            val gainedXpBase = xpByAction[actionType] ?: 0L
            val gainedXp = if (actionType == UserActionType.SEND_GROUP_MESSAGE) {
                val today = LocalDate.now(ZoneOffset.UTC).toString()
                val storedDay = userSnapshot.getString(socialXpDayField)
                val todaySocialXp = if (storedDay == today) userSnapshot.getLong(socialXpTodayField) ?: 0L else 0L
                val remaining = (socialXpDailyCap - todaySocialXp).coerceAtLeast(0L)
                val xpToGrant = gainedXpBase.coerceAtMost(remaining)
                transaction.update(
                    userRef,
                    mapOf(
                        socialXpDayField to today,
                        socialXpTodayField to (todaySocialXp + xpToGrant)
                    )
                )
                xpToGrant
            } else {
                gainedXpBase
            }
            if (gainedXp > 0) {
                // `totalXp` é uma projeção agregada derivada da trilha em `user_actions`.
                // Atualizamos por incremento para leitura rápida em perfil/ranking.
                transaction.update(userRef, "totalXp", currentXp + gainedXp)
            }

            when (actionType) {
                // Estes contadores são projeções derivadas de `user_actions` e podem ser
                // recomputados pela rotina administrativa de reconciliação.
                UserActionType.RATE_BOOK -> transaction.update(userRef, "ratedBooksCount", ratedCount + 1)
                UserActionType.MARK_BOOK_AS_FINISHED -> transaction.update(userRef, "finishedBooksCount", finishedCount + 1)
                UserActionType.SEND_GROUP_MESSAGE -> transaction.update(userRef, "groupMessageCount", messageCount + 1)
                UserActionType.READING_CHECKIN -> transaction.update(userRef, "readingCheckinCount", checkinCount + 1)
                else -> Unit
            }

            transaction.set(
                actionRef,
                mapOf(
                    "userId" to userId,
                    "actionType" to actionType.name,
                    "metadata" to metadata,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            true
        }.await()

        if (!applied) return false

        unlockTitlesIfEligible(userId)
        achievementService.evaluateAndPersist(userId, actionType)
        Log.d(tag, "Ação processada: user=$userId action=$actionType id=$idempotencyKey")
        return true
    }

    /**
     * Rotina administrativa para reconciliar as projeções do usuário a partir da trilha
     * canônica de eventos em `user_actions`.
     */
    suspend fun recomputeUserProjection(userId: String) {
        val userRef = db.collection("users").document(userId)
        val actions = db.collection("user_actions")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents

        var totalXp = 0L
        var ratedCount = 0L
        var finishedCount = 0L
        var messageCount = 0L
        var checkinCount = 0L
        val checkinDates = mutableSetOf<LocalDate>()

        actions.forEach { doc ->
            val actionName = doc.getString("actionType") ?: return@forEach
            val actionType = runCatching { UserActionType.valueOf(actionName) }.getOrNull() ?: return@forEach

            val xpForAction = if (actionType == UserActionType.SEND_GROUP_MESSAGE) 0L else (xpByAction[actionType] ?: 0L)
            totalXp += xpForAction
            when (actionType) {
                UserActionType.RATE_BOOK -> ratedCount++
                UserActionType.MARK_BOOK_AS_FINISHED -> finishedCount++
                UserActionType.SEND_GROUP_MESSAGE -> messageCount++
                UserActionType.READING_CHECKIN -> {
                    checkinCount++
                    val createdAt = doc.getTimestamp("createdAt")
                    createdAt?.toLocalDateUtc()?.let { checkinDates.add(it) }
                }
                else -> Unit
            }
        }

        userRef.update(
            mapOf(
                "totalXp" to totalXp,
                "ratedBooksCount" to ratedCount,
                "finishedBooksCount" to finishedCount,
                "groupMessageCount" to messageCount,
                "readingCheckinCount" to checkinCount,
                "currentStreak" to calculateCurrentStreak(checkinDates)
            )
        ).await()
    }

    /** Reprocessa projeções agregadas de todos os usuários cadastrados. */
    suspend fun recomputeAllUsersProjections() {
        val users = db.collection("users").get().await().documents
        users.forEach { userDoc ->
            recomputeUserProjection(userDoc.id)
        }
    }

    private fun Timestamp.toLocalDateUtc(): LocalDate =
        Instant.ofEpochSecond(seconds, nanoseconds.toLong()).atZone(ZoneOffset.UTC).toLocalDate()

    private fun calculateCurrentStreak(checkinDates: Set<LocalDate>): Long {
        if (checkinDates.isEmpty()) return 0L
        var streak = 0L
        var cursor = LocalDate.now(ZoneOffset.UTC)

        while (checkinDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private suspend fun unlockTitlesIfEligible(userId: String) {
        val userRef = db.collection("users").document(userId)
        val snapshot = userRef.get().await()
        val finishedCount = snapshot.getLong("finishedBooksCount") ?: 0L
        val ratedCount = snapshot.getLong("ratedBooksCount") ?: 0L
        val messageCount = snapshot.getLong("groupMessageCount") ?: 0L
        val checkinCount = snapshot.getLong("readingCheckinCount") ?: 0L

        val eligibleTitles = mutableListOf<String>()
        if (finishedCount >= 1) eligibleTitles.add("Leitor Iniciante")
        if (ratedCount >= 5) eligibleTitles.add("Crítico Literário")
        if (messageCount >= 20) eligibleTitles.add("Participante Ativo")
        if (checkinCount >= 7) eligibleTitles.add("Leitor Constante")

        eligibleTitles.forEach { titleName ->
            val titleDoc = userRef.collection("titles").document(titleName)
            val existing = titleDoc.get().await()
            if (!existing.exists()) {
                titleDoc.set(
                    mapOf(
                        "userId" to userId,
                        "titleName" to titleName,
                        "unlockedAt" to FieldValue.serverTimestamp(),
                        "isEquipped" to false
                    )
                ).await()
            }
        }
    }
}
