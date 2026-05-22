package br.com.CapitularIA.services

import br.com.CapitularIA.data.AchievementDefinition
import br.com.CapitularIA.data.AchievementRarity
import br.com.CapitularIA.data.AchievementType
import br.com.CapitularIA.data.UserActionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AchievementCatalog {
    val initial: List<AchievementDefinition> = listOf(
        AchievementDefinition("reader_first_book", "Leitor Iniciante", "Finalize seu primeiro livro.", "Concluir 1 livro", "📚", AchievementType.BOOKS, AchievementRarity.COMMON, 1),
        AchievementDefinition("reader_10_books", "Devorador de Histórias", "Você realmente ama livros.", "Concluir 10 livros", "📕", AchievementType.BOOKS, AchievementRarity.RARE, 10),
        AchievementDefinition("reader_25_books", "Biblioteca Ambulante", "Seu conhecimento está crescendo.", "Concluir 25 livros", "📚", AchievementType.BOOKS, AchievementRarity.EPIC, 25),
        AchievementDefinition("critic_first", "Primeira Opinião", "Avalie seu primeiro livro.", "Avaliar 1 livro", "⭐", AchievementType.RATINGS, AchievementRarity.COMMON, 1),
        AchievementDefinition("critic_5", "Crítico Iniciante", "Compartilhe suas opiniões.", "Avaliar 5 livros", "🌟", AchievementType.RATINGS, AchievementRarity.UNCOMMON, 5),
        AchievementDefinition("critic_25", "Crítico Literário", "Sua opinião influencia leitores.", "Avaliar 25 livros", "🧠", AchievementType.RATINGS, AchievementRarity.RARE, 25),
        AchievementDefinition("critic_50", "Curador Literário", "Você entende de livros.", "Avaliar 50 livros", "👑", AchievementType.RATINGS, AchievementRarity.LEGENDARY, 50),
        AchievementDefinition("social_first_message", "Primeira Mensagem", "Participe de uma conversa.", "Enviar 1 mensagem", "💬", AchievementType.SOCIAL, AchievementRarity.COMMON, 1),
        AchievementDefinition("social_20_messages", "Participante Ativo", "Os clubes conhecem você.", "Enviar 20 mensagens", "🗣️", AchievementType.SOCIAL, AchievementRarity.UNCOMMON, 20),
        AchievementDefinition("social_100_messages", "Voz da Comunidade", "Você movimenta discussões.", "Enviar 100 mensagens", "🎙️", AchievementType.SOCIAL, AchievementRarity.RARE, 100),
        AchievementDefinition("checkin_25", "Virador de Páginas", "Mantenha o ritmo de leitura.", "Registrar 25 check-ins", "📘", AchievementType.CONSISTENCY, AchievementRarity.UNCOMMON, 25),
        AchievementDefinition("streak_7", "Leitor Constante", "Mantenha o hábito de leitura.", "7 check-ins seguidos", "🔥", AchievementType.CONSISTENCY, AchievementRarity.UNCOMMON, 7),
        AchievementDefinition("streak_30", "Chama Literária", "Você criou disciplina.", "30 dias seguidos", "🔥", AchievementType.CONSISTENCY, AchievementRarity.RARE, 30),
        AchievementDefinition("streak_100", "Ritual da Leitura", "Ler já faz parte da sua vida.", "100 dias seguidos", "☀️", AchievementType.CONSISTENCY, AchievementRarity.LEGENDARY, 100),
        AchievementDefinition("ai_first_use", "Primeiro Conselho", "Use a IA pela primeira vez.", "Usar IA 1 vez", "🤖", AchievementType.AI, AchievementRarity.COMMON, 1),
        AchievementDefinition("ai_10_uses", "Explorador de Histórias", "Descubra novos livros.", "Usar IA 10 vezes", "🧠", AchievementType.AI, AchievementRarity.UNCOMMON, 10),
        AchievementDefinition("ai_50_uses", "Oráculo Literário", "Você domina o guia artificial.", "Usar IA 50 vezes", "🔮", AchievementType.AI, AchievementRarity.EPIC, 50)
    )

    fun forAction(actionType: UserActionType): List<AchievementDefinition> = when (actionType) {
        UserActionType.MARK_BOOK_AS_FINISHED -> initial.filter { it.type == AchievementType.BOOKS }
        UserActionType.RATE_BOOK -> initial.filter { it.type == AchievementType.RATINGS }
        UserActionType.SEND_GROUP_MESSAGE -> initial.filter { it.type == AchievementType.SOCIAL }
        UserActionType.READING_CHECKIN -> initial.filter { it.type == AchievementType.CONSISTENCY || it.id.startsWith("streak_") }
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
            val currentStreak = userSnapshot.getLong("currentStreak") ?: 0L
            val aiUsageCount = userSnapshot.getLong("aiUsageCount") ?: 0L

            achievements.forEach { definition ->
                val achievementDoc = userAchievementRef.document(definition.id)
                val currentProgress = when (definition.id) {
                    "streak_7", "streak_30", "streak_100" -> currentStreak
                    else -> when (definition.type) {
                        AchievementType.BOOKS -> finishedCount
                        AchievementType.RATINGS -> ratedCount
                        AchievementType.SOCIAL -> messageCount
                        AchievementType.CONSISTENCY -> checkinCount
                        AchievementType.AI -> aiUsageCount
                        AchievementType.SECRET -> 0L
                    }
                }

                val isSecretLocked = definition.type == AchievementType.SECRET && currentProgress <= 0L
                val unlocked = !isSecretLocked && currentProgress >= definition.requiredProgress
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
