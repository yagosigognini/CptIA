package br.com.CapitularIA.services

import br.com.CapitularIA.data.UserActionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GamificationService(
    private val db: FirebaseFirestore,
    private val achievementService: AchievementService = AchievementService(db)
) {

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
            val currentXp = userSnapshot.getLong("totalXp") ?: 0L
            val finishedCount = userSnapshot.getLong("finishedBooksCount") ?: 0L
            val ratedCount = userSnapshot.getLong("ratedBooksCount") ?: 0L
            val messageCount = userSnapshot.getLong("groupMessageCount") ?: 0L
            val checkinCount = userSnapshot.getLong("readingCheckinCount") ?: 0L

            val gainedXp = xpByAction[actionType] ?: 0L
            if (gainedXp > 0) {
                transaction.update(userRef, "totalXp", currentXp + gainedXp)
            }

            when (actionType) {
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
        return true
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
