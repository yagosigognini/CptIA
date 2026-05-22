package br.com.CapitularIA.services

import br.com.CapitularIA.data.AiBookRecommendation
import br.com.CapitularIA.data.BookItem
import br.com.CapitularIA.data.ClubRecommendationContext
import br.com.CapitularIA.data.RecommendationRequest
import br.com.CapitularIA.data.ValidatedRecommendation
import br.com.CapitularIA.network.RetrofitInstance
import br.com.CapitularIA.services.search.BookSearchQueryBuilder
import java.text.Normalizer

class ClubBookRecommendationEngine(
    private val recommendationApiService: RecommendationApiService = RecommendationApiService()
) {

    suspend fun recommendBooks(
        context: ClubRecommendationContext,
        request: RecommendationRequest,
        recentRecommendationTitles: List<String>
    ): List<ValidatedRecommendation> {
        val aiRecommendations = recommendationApiService.recommend(context, request, recentRecommendationTitles)
            .filterNot { ai -> context.readBooks.any { it.title.equals(ai.title, ignoreCase = true) } }

        val validatedFromAi = aiRecommendations
            .mapNotNull { ai -> validateWithGoogleBooks(ai) }
            .distinctBy { it.bookItem.id ?: it.recommendation.title.lowercase() }
            .take(request.maxResults)

        if (validatedFromAi.isNotEmpty()) return validatedFromAi

        // Se a IA retornou sugestões, evitamos cair no fallback amplo do Google Books
        // para não perder aderência ao pedido do usuário.
        if (aiRecommendations.isNotEmpty()) return emptyList()

        return fallbackRecommendations(context, request, recentRecommendationTitles)
    }

    private suspend fun fallbackRecommendations(
        context: ClubRecommendationContext,
        request: RecommendationRequest,
        recentRecommendationTitles: List<String>
    ): List<ValidatedRecommendation> {
        val theme = request.userPrompt.takeIf { it.isNotBlank() }
            ?: context.predominantGenres.firstOrNull()
            ?: context.recurringTags.firstOrNull()
            ?: "ficção"

        val queryBuild = BookSearchQueryBuilder.build(theme)
        if (queryBuild.primaryQuery.isBlank()) return emptyList()

        val primaryResponse = RetrofitInstance.api.searchBooks(
            query = queryBuild.primaryQuery,
            maxResults = 20,
            langRestrict = "pt",
            printType = "books",
            orderBy = "relevance",
            projection = "full",
            apiKey = RetrofitInstance.apiKey
        )

        var books = primaryResponse.body()?.items.orEmpty()
        if (books.isEmpty()) {
            val fallbackResponse = RetrofitInstance.api.searchBooks(
                query = queryBuild.fallbackQuery,
                maxResults = 20,
                langRestrict = "pt",
                printType = "books",
                orderBy = "relevance",
                projection = "full",
                apiKey = RetrofitInstance.apiKey
            )
            books = fallbackResponse.body()?.items.orEmpty()
        }

        val readAndRecentKeys = (context.readBooks.map { it.title to it.author } +
            recentRecommendationTitles.map { it to "" })
            .map { (title, author) -> normalizedBookKey(title, author) }
            .toSet()

        val scored = books
            .mapNotNull { item ->
                val score = calculateFallbackBookScore(item, queryBuild.normalizedTerm)
                if (score.shouldDiscardByCoreMetadata) null else ScoredFallbackBook(item, score.score)
            }
            .sortedByDescending { it.score }

        return scored
            .asSequence()
            .map { it.book }
            .distinctBy { it.id ?: normalizedBookKey(it.volumeInfo?.title.orEmpty(), it.volumeInfo?.authors.orEmpty().joinToString(" ")) }
            .mapNotNull { item ->
                val title = item.volumeInfo?.title.orEmpty()
                val author = item.volumeInfo?.authors?.joinToString().orEmpty()
                val bookKey = normalizedBookKey(title, author)
                if (title.isBlank() || readAndRecentKeys.contains(bookKey)) {
                    null
                } else {
                    val reason = "Sugestão baseada no tema \"$theme\" e no perfil do clube."
                    ValidatedRecommendation(
                        recommendation = AiBookRecommendation(title = title, author = author, reason = reason),
                        bookItem = item
                    )
                }
            }
            .take(request.maxResults)
            .toList()
    }

    private suspend fun validateWithGoogleBooks(aiRecommendation: AiBookRecommendation): ValidatedRecommendation? {
        val query = buildString {
            append("intitle:${aiRecommendation.title}")
            if (aiRecommendation.author.isNotBlank()) {
                append("+inauthor:${aiRecommendation.author}")
            }
        }

        val response = RetrofitInstance.api.searchBooks(
            query = query,
            maxResults = 3,
            apiKey = RetrofitInstance.apiKey
        )

        val expectedTitle = aiRecommendation.title.normalizedForMatch()
        val expectedAuthor = aiRecommendation.author.normalizedForMatch()

        val validated: BookItem = response.body()?.items
            ?.firstOrNull { item ->
                val title = item.volumeInfo?.title.orEmpty().normalizedForMatch()
                val authors = item.volumeInfo?.authors.orEmpty().joinToString(" ").normalizedForMatch()

                val titleMatches = title == expectedTitle ||
                    title.contains(expectedTitle) ||
                    expectedTitle.contains(title)

                val authorMatches = expectedAuthor.isBlank() ||
                    authors.contains(expectedAuthor) ||
                    expectedAuthor.contains(authors)

                titleMatches && authorMatches
            }
            ?: return null

        return ValidatedRecommendation(aiRecommendation, validated)
    }
}

private data class ScoredFallbackBook(
    val book: BookItem,
    val score: Int
)

private data class FallbackBookScoreResult(
    val score: Int,
    val shouldDiscardByCoreMetadata: Boolean
)

private fun calculateFallbackBookScore(book: BookItem, normalizedTerm: String): FallbackBookScoreResult {
    val volumeInfo = book.volumeInfo
    val normalizedTitle = normalizeForFallback(volumeInfo?.title)
    val authors = volumeInfo?.authors.orEmpty()
    val normalizedAuthors = authors.joinToString(" ") { normalizeForFallback(it) }
    val hasMissingCoreMetadata = normalizedTitle.isBlank() || authors.isEmpty()
    val titleExactMatch = normalizedTitle.isNotBlank() && normalizedTitle == normalizedTerm
    val titlePartialMatch = normalizedTitle.isNotBlank() && normalizedTitle.contains(normalizedTerm)
    val authorMatch = normalizedAuthors.isNotBlank() && normalizedAuthors.contains(normalizedTerm)

    var score = 0
    if (titleExactMatch) score += 120
    if (titlePartialMatch) score += 70
    if (authorMatch) score += 40

    return FallbackBookScoreResult(
        score = score,
        shouldDiscardByCoreMetadata = hasMissingCoreMetadata
    )
}

private fun normalizedBookKey(title: String?, author: String?): String {
    val normalizedTitle = normalizeForFallback(title)
    val normalizedAuthor = normalizeForFallback(author)
    return "$normalizedTitle::$normalizedAuthor"
}

private fun normalizeForFallback(value: String?): String =
    Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
        .trim()

private fun String.normalizedForMatch(): String =
    normalizeForFallback(this)
