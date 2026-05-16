package br.com.CapitularIA.data

data class BookClub(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isPublic: Boolean = true,
    val code: String? = null,
    val adminId: String = "",
    val members: List<String> = emptyList(),
    val maxMembers: Int = 10,
    var readingCycleDays: Int = 15, // Deixamos como 'var' para poder ser alterado

    // --- Campos do Ciclo de Leitura ---
    val currentUserForCycleId: String? = null,
    val indicatedBook: IndicatedBook? = null, // Substitui o 'indicatedBookTitle'
    val cycleStartDate: Long? = null,
    val cycleEndDate: Long? = null,
    val readingHistory: List<ReadingHistoryEntry> = emptyList(),

    // --- Campo das Solicitações ---
    val joinRequests: List<String> = emptyList(),

    // --- Preferências literárias do clube ---
    val preferredGenres: List<String> = emptyList(),
    val preferredTags: List<String> = emptyList(),

    val createdAt: Long = System.currentTimeMillis()
)
