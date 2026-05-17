package br.com.CapitularIA.services

import br.com.CapitularIA.data.AiBookRecommendation
import br.com.CapitularIA.data.BookItem
import br.com.CapitularIA.data.ClubRecommendationContext
import br.com.CapitularIA.data.RecommendationRequest
import br.com.CapitularIA.data.ValidatedRecommendation
import br.com.CapitularIA.network.RetrofitInstance
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

        val response = RetrofitInstance.api.searchBooks(
            query = theme,
            maxResults = 12,
            apiKey = RetrofitInstance.apiKey
        )

        return response.body()?.items
            .orEmpty()
            .asSequence()
            .filter { item ->
                val title = item.volumeInfo?.title.orEmpty()
                title.isNotBlank() &&
                    context.readBooks.none { it.title.equals(title, ignoreCase = true) } &&
                    recentRecommendationTitles.none { it.equals(title, ignoreCase = true) }
            }
            .map { item ->
                val title = item.volumeInfo?.title.orEmpty()
                val author = item.volumeInfo?.authors?.joinToString().orEmpty()
                val reason = "Sugestão baseada no tema \"$theme\" e no perfil do clube."
                ValidatedRecommendation(
                    recommendation = AiBookRecommendation(title = title, author = author, reason = reason),
                    bookItem = item
                )
            }
            .distinctBy { it.bookItem.id ?: it.recommendation.title.lowercase() }
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

private fun String.normalizedForMatch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
