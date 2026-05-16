package br.com.CapitularIA.services

import br.com.CapitularIA.BuildConfig
import br.com.CapitularIA.data.AiBookRecommendation
import br.com.CapitularIA.data.BookHistory
import br.com.CapitularIA.data.ClubRecommendationContext
import br.com.CapitularIA.data.RecommendationRequest
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiRecommendationService(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    fun recommend(
        context: ClubRecommendationContext,
        request: RecommendationRequest,
        recentRecommendationTitles: List<String>
    ): List<AiBookRecommendation> {
        if (apiKey.isBlank()) return emptyList()

        val prompt = buildPrompt(context, request, recentRecommendationTitles)
        val payload = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", prompt) })
                    })
                })
            })
        }

        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string().orEmpty()
            return parseRecommendations(body)
        }
    }

    private fun buildPrompt(
        context: ClubRecommendationContext,
        request: RecommendationRequest,
        recentRecommendationTitles: List<String>
    ): String {
        return """
Você é um especialista literário para clubes de leitura.
Responda SOMENTE em JSON válido, sem markdown.
Formato: [{"title":"","author":"","reason":""}]

Contexto do clube:
- Nome: ${context.clubName}
- Gêneros predominantes: ${context.predominantGenres.joinToString()}
- Tags recorrentes: ${context.recurringTags.joinToString()}
- Avaliação média: ${context.averageRating ?: "N/A"}
- Livros já lidos: ${context.readBooks.joinBookList()}
- Livros favoritos: ${context.favoriteBooks.joinBookList()}
- Recomendações recentes a evitar: ${recentRecommendationTitles.joinToString()}

Solicitação do usuário:
"${request.userPrompt}"

Regras:
- Não recomendar livros já lidos
- Não repetir recomendações recentes
- Priorizar diversidade entre autores/subgêneros
- Recomendar no máximo ${request.maxResults} livros
- Justificar cada recomendação em até 180 caracteres
- Evitar inventar títulos
""".trimIndent()
    }

    private fun parseRecommendations(rawResponse: String): List<AiBookRecommendation> {
        return runCatching {
            val root = gson.fromJson(rawResponse, JsonObject::class.java)
            val text = root
                .getAsJsonArray("candidates")
                ?.firstOrNull()
                ?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.firstOrNull()
                ?.asJsonObject
                ?.get("text")
                ?.asString
                .orEmpty()

            val json = gson.fromJson(text, JsonArray::class.java)
            json.mapNotNull { el ->
                val obj = el.asJsonObject
                val title = obj.get("title")?.asString.orEmpty().trim()
                val author = obj.get("author")?.asString.orEmpty().trim()
                val reason = obj.get("reason")?.asString.orEmpty().trim()
                if (title.isBlank()) null else AiBookRecommendation(title, author, reason)
            }
        }.getOrDefault(emptyList())
    }
}

private fun List<BookHistory>.joinBookList(): String =
    if (isEmpty()) "nenhum" else joinToString { it.title }
