package br.com.CapitularIA.services

import br.com.CapitularIA.data.AchievementDefinition
import br.com.CapitularIA.data.AchievementType
import br.com.CapitularIA.data.UserActionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AchievementCatalog {
    val initial: List<AchievementDefinition> = listOf(
        AchievementDefinition("first_finished_book", "Primeiro Livro Finalizado", "Finalize seu primeiro livro.", "Concluir 1 livro", "📘", AchievementType.BOOKS, 1),
        AchievementDefinition("beginner_critic", "Crítico Iniciante", "Avalie livros para compartilhar sua opinião.", "Avaliar 5 livros", "⭐", AchievementType.RATINGS, 5),
        AchievementDefinition("active_member", "Participante Ativo", "Participe das conversas nos clubes.", "Enviar 20 mensagens", "💬", AchievementType.SOCIAL, 20),
        AchievementDefinition("consistent_reader", "Leitor Constante", "Mantenha o hábito de leitura.", "Registrar 7 check-ins", "🔥", AchievementType.CONSISTENCY, 7)
    )

    fun forAction(actionType: UserActionType): List<AchievementDefinition> = when (actionType) {
        UserActionType.MARK_BOOK_AS_FINISHED -> initial.filter { it.id == "first_finished_book" }
        UserActionType.RATE_BOOK -> initial.filter { it.id == "beginner_critic" }
        UserActionType.SEND_GROUP_MESSAGE -> initial.filter { it.id == "active_member" }
        UserActionType.READING_CHECKIN -> initial.filter { it.id == "consistent_reader" }
        else -> emptyList()
    }
}

class AchievementService(private val db: FirebaseFirestore) {

    suspend fun evaluateAndPersist(userId: String, actionType: UserActionType) {
        val achievements = AchievementCatalog.forAction(actionType)
        if (achievements.isEmpty()) return

        val userRef = db.collection("users").document(userId)
        val userAchievementRef = userRef.collection("achievements")

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val finishedCount = userSnapshot.getLong("finishedBooksCount") ?: 0L
            val ratedCount = userSnapshot.getLong("ratedBooksCount") ?: 0L
            val messageCount = userSnapshot.getLong("groupMessageCount") ?: 0L
            val checkinCount = userSnapshot.getLong("readingCheckinCount") ?: 0L

            achievements.forEach { definition ->
                val currentProgress = when (definition.id) {
                    "first_finished_book" -> finishedCount
                    "beginner_critic" -> ratedCount
                    "active_member" -> messageCount
                    "consistent_reader" -> checkinCount
                    else -> 0L
                }

                val unlocked = currentProgress >= definition.requiredProgress
                val achievementDoc = userAchievementRef.document(definition.id)
                val payload = hashMapOf<String, Any>(
                    "userId" to userId,
                    "achievementId" to definition.id,
                    "progress" to currentProgress,
                    "unlocked" to unlocked,
                    "updatedAtEpochMillis" to System.currentTimeMillis()
                )
                if (unlocked) payload["unlockedAt"] = FieldValue.serverTimestamp()
                transaction.set(achievementDoc, payload)
            }
        }.await()
    }
}
