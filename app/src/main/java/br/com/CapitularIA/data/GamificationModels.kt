package br.com.CapitularIA.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

enum class UserActionType {
    LOGIN_APP,
    USE_AI_RECOMMENDATION,
    SEND_GROUP_MESSAGE,
    RATE_BOOK,
    ADD_BOOK_TO_SHELF,
    MARK_BOOK_AS_READING,
    MARK_BOOK_AS_FINISHED,
    READING_CHECKIN
}

enum class ReadingStatus(val label: String) {
    WANT_TO_READ("Quero ler"),
    READING("Lendo"),
    FINISHED("Lido"),
    ABANDONED("Abandonei")
}

data class UserAction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actionType: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val metadata: Map<String, Any> = emptyMap()
)

data class UserTitle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleName: String = "",
    @ServerTimestamp val unlockedAt: Timestamp? = null,
    val isEquipped: Boolean = false
)

data class ReadingCheckin(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookId: String? = null,
    val pagesRead: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class AchievementType {
    BOOKS,
    RATINGS,
    SOCIAL,
    CONSISTENCY,
    AI,
    SECRET
}

enum class AchievementRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val criteria: String,
    val icon: String,
    val type: AchievementType,
    val actionType: UserActionType? = null,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
    val requiredProgress: Long
)

data class UserAchievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val achievementId: String = "",
    val progress: Long = 0,
    val unlocked: Boolean = false,
    @ServerTimestamp val unlockedAt: Timestamp? = null,
    val updatedAtEpochMillis: Long = 0
)
