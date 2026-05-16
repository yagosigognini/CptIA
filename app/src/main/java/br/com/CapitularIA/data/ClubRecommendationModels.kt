package br.com.CapitularIA.data

data class ClubRecommendationContext(
    val clubId: String,
    val clubName: String,
    val predominantGenres: List<String>,
    val readBooks: List<BookHistory>,
    val favoriteBooks: List<BookHistory>,
    val recurringTags: List<String>,
    val averageRating: Float?
)

data class BookHistory(
    val googleBooksId: String,
    val title: String,
    val rating: Float?
)

data class UserPreferences(
    val favoriteGenres: List<String> = emptyList(),
    val favoriteAuthors: List<String> = emptyList()
)

data class RecommendationRequest(
    val userPrompt: String,
    val maxResults: Int = 5,
    val tone: String? = null
)

data class AiBookRecommendation(
    val title: String,
    val author: String,
    val reason: String
)

data class ValidatedRecommendation(
    val recommendation: AiBookRecommendation,
    val bookItem: BookItem
)
