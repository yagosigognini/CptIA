package br.com.CapitularIA.services

import br.com.CapitularIA.data.AiBookRecommendation
import br.com.CapitularIA.data.BookItem
import br.com.CapitularIA.data.ClubRecommendationContext
import br.com.CapitularIA.data.RecommendationRequest
import br.com.CapitularIA.data.ValidatedRecommendation
import br.com.CapitularIA.network.RetrofitInstance

class ClubBookRecommendationEngine(
    private val geminiService: GeminiRecommendationService = GeminiRecommendationService()
) {

    suspend fun recommendBooks(
        context: ClubRecommendationContext,
        request: RecommendationRequest,
        recentRecommendationTitles: List<String>
    ): List<ValidatedRecommendation> {
        val aiRecommendations = geminiService.recommend(context, request, recentRecommendationTitles)
            .filterNot { ai -> context.readBooks.any { it.title.equals(ai.title, ignoreCase = true) } }

        return aiRecommendations
            .mapNotNull { ai -> validateWithGoogleBooks(ai) }
            .distinctBy { it.bookItem.id ?: it.recommendation.title.lowercase() }
            .take(request.maxResults)
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

        val validated: BookItem = response.body()?.items
            ?.firstOrNull { item ->
                val title = item.volumeInfo?.title.orEmpty()
                title.equals(aiRecommendation.title, ignoreCase = true)
            }
            ?: return null

        return ValidatedRecommendation(aiRecommendation, validated)
    }
}
