package br.com.CapitularIA.services

import br.com.CapitularIA.data.AchievementDefinition
import br.com.CapitularIA.data.AchievementType
import br.com.CapitularIA.data.UserActionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AchievementCatalog {
    val initial: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "reader_first_book",
            name = "Primeiro Livro Finalizado",
            description = "Finalize seu primeiro livro.",
            criteria = "Concluir 1 livro",
            icon = "📘",
            type = AchievementType.BOOKS,
            actionType = UserActionType.MARK_BOOK_AS_FINISHED,
            requiredProgress = 1
        ),
        AchievementDefinition(
            id = "critic_5",
            name = "Crítico Iniciante",
            description = "Avalie livros para compartilhar sua opinião.",
            criteria = "Avaliar 5 livros",
            icon = "⭐",
            type = AchievementType.RATINGS,
            actionType = UserActionType.RATE_BOOK,
            requiredProgress = 5
        ),
        AchievementDefinition(
            id = "social_20_messages",
            name = "Participante Ativo",
            description = "Participe das conversas nos clubes.",
            criteria = "Enviar 20 mensagens",
            icon = "💬",
            type = AchievementType.SOCIAL,
            actionType = UserActionType.SEND_GROUP_MESSAGE,
            requiredProgress = 20
        ),
        AchievementDefinition(
            id = "checkin_7",
            name = "Leitor Constante",
            description = "Mantenha o hábito de leitura.",
            criteria = "Registrar 7 check-ins",
            icon = "🔥",
            type = AchievementType.CONSISTENCY,
            actionType = UserActionType.READING_CHECKIN,
            requiredProgress = 7
        )
    )

    fun forAction(actionType: UserActionType): List<AchievementDefinition> =
        initial.filter { it.actionType == actionType }
}

class AchievementService(private val db: FirebaseFirestore) {

    suspend fun evaluateAndPersist(userId: String, actionType: UserActionType) {
        val achievements = AchievementCatalog.forAction(actionType)
        if (achievements.isEmpty()) return

        val userRef = db.collection("users").document(userId)
        val userAchievementRef = userRef.collection("achievements")
        val userActions = db.collection("user_actions")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
        val actionCountByType = userActions
            .mapNotNull { actionDoc ->
                actionDoc.getString("actionType")
                    ?.let { runCatching { UserActionType.valueOf(it) }.getOrNull() }
            }
            .groupingBy { it }
            .eachCount()
            .mapValues { it.value.toLong() }

        db.runTransaction { transaction ->
            achievements.forEach { definition ->
                val achievementDoc = userAchievementRef.document(definition.id)
                val currentProgress = definition.actionType?.let { actionCountByType[it] } ?: 0L
                val unlocked = currentProgress >= definition.requiredProgress
                val previousUnlocked = transaction.get(achievementDoc).getBoolean("unlocked") ?: false
                val shouldSetUnlockedAt = unlocked && !previousUnlocked
                val payload = hashMapOf<String, Any>(
                    "userId" to userId,
                    "achievementId" to definition.id,
                    "progress" to currentProgress,
                    "unlocked" to unlocked,
                    "updatedAtEpochMillis" to System.currentTimeMillis()
                )
                if (shouldSetUnlockedAt) payload["unlockedAt"] = FieldValue.serverTimestamp()
                transaction.set(achievementDoc, payload)
            }
        }.await()
    }
}
