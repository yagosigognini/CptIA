package br.com.CapitularIA.services

import br.com.CapitularIA.BuildConfig
import br.com.CapitularIA.data.AiBookRecommendation
import br.com.CapitularIA.data.ClubRecommendationContext
import br.com.CapitularIA.data.RecommendationRequest
import okhttp3.OkHttpClient
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecommendationApiService(
    private val api: RecommendationBackendApi = RecommendationBackendRetrofit.api
) {

    suspend fun recommend(
        context: ClubRecommendationContext,
        request: RecommendationRequest,
        recentRecommendationTitles: List<String>
    ): List<AiBookRecommendation> {
        return runCatching {
            api.recommend(
                RecommendationApiPayload(
                    context = context,
                    request = request,
                    recentRecommendationTitles = recentRecommendationTitles
                )
            ).recommendations
        }.getOrDefault(emptyList())
    }
}

data class RecommendationApiPayload(
    val context: ClubRecommendationContext,
    val request: RecommendationRequest,
    val recentRecommendationTitles: List<String>
)

data class RecommendationApiResponse(
    val recommendations: List<AiBookRecommendation> = emptyList()
)

interface RecommendationBackendApi {
    @POST("recommendations")
    suspend fun recommend(@Body payload: RecommendationApiPayload): RecommendationApiResponse
}

private object RecommendationBackendRetrofit {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.RECOMMENDATION_API_BASE_URL))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: RecommendationBackendApi by lazy { retrofit.create(RecommendationBackendApi::class.java) }

    private fun normalizeBaseUrl(value: String): String {
        val base = value.trim().ifBlank { "http://10.0.2.2:8080/" }
        return if (base.endsWith('/')) base else "$base/"
    }
}
