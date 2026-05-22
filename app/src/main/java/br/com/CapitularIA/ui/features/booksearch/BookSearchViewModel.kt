package br.com.CapitularIA.ui.features.booksearch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.BuildConfig
import br.com.CapitularIA.data.BookItem // Importa o modelo do livro
import br.com.CapitularIA.data.getBestAvailableImageUrl
import br.com.CapitularIA.network.RetrofitInstance // Importa nossa instância do Retrofit
import br.com.CapitularIA.services.search.BookSearchQueryBuilder
import java.text.Normalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Define os possíveis estados da busca
sealed class BookSearchState {
    data object Idle : BookSearchState() // Estado inicial ou após limpar
    data object Loading : BookSearchState() // Buscando na API
    data class Success(val books: List<BookItem>) : BookSearchState() // Sucesso, com a lista de livros
    data class Error(val message: String) : BookSearchState() // Erro na busca
}

class BookSearchViewModel : ViewModel() {

    companion object {
        private const val MAX_BOOK_RESULTS = 12
        private const val STRONG_RESULT_MIN_SCORE = 10
    }

    private val apiService = RetrofitInstance.api // Pega a instância da API
    private val apiKey = RetrofitInstance.apiKey // Pega a chave da API

    // StateFlow para expor o estado da busca para a UI
    private val _searchState = MutableStateFlow<BookSearchState>(BookSearchState.Idle)
    val searchState: StateFlow<BookSearchState> = _searchState

    /**
     * Inicia a busca de livros na API do Google Books.
     * @param query O termo a ser buscado (título, autor, etc.).
     */
    fun searchBooks(query: String) {
        val queryBuild = BookSearchQueryBuilder.build(query)
        if (queryBuild.primaryQuery.isBlank()) {
            _searchState.value = BookSearchState.Idle // Limpa se a busca for vazia
            return
        }

        viewModelScope.launch {
            _searchState.value = BookSearchState.Loading // Avisa a UI que está carregando
            try {
                // Busca principal com filtros para melhorar precisão
                val response = apiService.searchBooks(
                    query = queryBuild.primaryQuery,
                    maxResults = 20,
                    langRestrict = "pt",
                    printType = "books",
                    orderBy = "relevance",
                    projection = "full",
                    apiKey = apiKey
                )

                if (response.isSuccessful) {
                    // Sucesso: Pega a lista de livros (items) da resposta
                    var books = response.body()?.items ?: emptyList()

                    // Fallback sem filtros caso a busca filtrada não retorne nada
                    if (books.isEmpty()) {
                        Log.d("BookSearchVM", "Busca filtrada vazia para '${queryBuild.primaryQuery}'. Tentando fallback sem filtros com '${queryBuild.fallbackQuery}'.")
                        val fallbackResponse = apiService.searchBooks(
                            query = queryBuild.fallbackQuery,
                            maxResults = 20,
                            apiKey = apiKey
                        )

                        if (fallbackResponse.isSuccessful) {
                            books = fallbackResponse.body()?.items ?: emptyList()
                            Log.d("BookSearchVM", "Fallback retornou ${books.size} livros para '${queryBuild.fallbackQuery}'.")
                        } else {
                            Log.w(
                                "BookSearchVM",
                                "Fallback falhou: ${fallbackResponse.code()} - ${fallbackResponse.message()}"
                            )
                        }
                    }

                    val scoredBooks = books
                        .mapNotNull { book ->
                            val scoreResult = calculateBookScore(book, queryBuild.normalizedTerm)
                            if (scoreResult.shouldDiscardByCoreMetadata) {
                                if (BuildConfig.DEBUG) {
                                    Log.d(
                                        "BookSearchVM",
                                        "Descartado por metadados centrais ausentes: title='${book.volumeInfo?.title}' authors=${book.volumeInfo?.authors}"
                                    )
                                }
                                null
                            } else {
                                if (BuildConfig.DEBUG) {
                                    Log.d(
                                        "BookSearchVM",
                                        "Score=${scoreResult.score} | titleExact=${scoreResult.titleExactMatch} " +
                                            "| titlePartial=${scoreResult.titlePartialMatch} | author=${scoreResult.authorMatch} " +
                                            "| image=${scoreResult.hasImage} | ratings=${scoreResult.hasRatings} " +
                                            "| outScope=${scoreResult.isClearlyOutOfScopeCategory} | title='${book.volumeInfo?.title}'"
                                    )
                                }
                                ScoredBook(book, scoreResult.score)
                            }
                        }
                        .sortedByDescending { it.score }

                    val strongResults = scoredBooks
                        .filter { it.score >= STRONG_RESULT_MIN_SCORE }
                        .take(MAX_BOOK_RESULTS)

                    val finalBooks = if (strongResults.isNotEmpty()) {
                        strongResults
                    } else {
                        scoredBooks.take(MAX_BOOK_RESULTS)
                    }.map { it.book }

                    Log.d("BookSearchVM", "Recebidos ${books.size} livros para '${queryBuild.primaryQuery}' (termo normalizado: '${queryBuild.normalizedTerm}'). Exibindo ${finalBooks.size} livros (fortes=${strongResults.size}, totalElegiveis=${scoredBooks.size}).")
                    finalBooks.take(3).forEachIndexed { index, item ->
                        Log.d("BookSearchVM", "Livro $index - Título: ${item.volumeInfo?.title}")
                        Log.d("BookSearchVM", "Livro $index - ImageLinks Object: ${item.volumeInfo?.imageLinks}")
                        Log.d("BookSearchVM", "Livro $index - Best URL: ${item.volumeInfo?.imageLinks.getBestAvailableImageUrl()}")
                    }

                    // Atualiza o estado da UI DEPOIS de logar
                    _searchState.value = BookSearchState.Success(finalBooks)
                    Log.d("BookSearchVM", "Busca bem-sucedida (estado atualizado) para '${queryBuild.primaryQuery}'") // Log original movido para depois da atualização do estado

                } else {
                    // Erro HTTP (ex: 404, 500)
                    val errorMsg = "Erro na API: ${response.code()} - ${response.message()}"
                    Log.e("BookSearchVM", errorMsg)
                    _searchState.value = BookSearchState.Error(errorMsg)
                }
            } catch (e: Exception) {
                // Erro de rede ou outro erro inesperado
                val errorMsg = "Falha na busca: ${e.message}"
                Log.e("BookSearchVM", errorMsg, e)
                _searchState.value = BookSearchState.Error(errorMsg)
            }
        }
    }

    /**
     * Reseta o estado da busca para Idle (útil para limpar a tela).
     */
    fun clearSearch() {
        _searchState.value = BookSearchState.Idle
    }

    private fun calculateBookScore(book: BookItem, normalizedTerm: String): BookScoreResult {
        val volumeInfo = book.volumeInfo
        val normalizedTitle = normalize(volumeInfo?.title)
        val authors = volumeInfo?.authors.orEmpty()
        val normalizedAuthors = authors.joinToString(" ") { normalize(it) }
        val hasImage = volumeInfo?.imageLinks.getBestAvailableImageUrl().isNotBlank()
        val hasRatings = (volumeInfo?.ratingsCount ?: 0) > 0 && (volumeInfo?.averageRating ?: 0.0) > 0.0

        val titleExactMatch = normalizedTitle.isNotBlank() && normalizedTitle == normalizedTerm
        val titlePartialMatch = normalizedTitle.isNotBlank() && normalizedTitle.contains(normalizedTerm)
        val authorMatch = normalizedAuthors.isNotBlank() && normalizedAuthors.contains(normalizedTerm)
        val hasMissingCoreMetadata = normalizedTitle.isBlank() || authors.isEmpty()
        val isSpecificQuery = normalizedTerm.length >= 6 || normalizedTerm.contains(" ")
        val isClearlyOutOfScopeCategory = isSpecificQuery && isClearlyOutOfScopeCategory(book, normalizedTerm)

        var score = 0
        if (titleExactMatch) score += 120
        if (titlePartialMatch) score += 70
        if (authorMatch) score += 40
        if (hasImage) score += 12
        if (hasRatings) score += 16
        if (!hasImage) score -= 8
        if (isClearlyOutOfScopeCategory) score -= 45

        return BookScoreResult(
            score = score,
            titleExactMatch = titleExactMatch,
            titlePartialMatch = titlePartialMatch,
            authorMatch = authorMatch,
            hasImage = hasImage,
            hasRatings = hasRatings,
            shouldDiscardByCoreMetadata = hasMissingCoreMetadata,
            isClearlyOutOfScopeCategory = isClearlyOutOfScopeCategory
        )
    }

    private fun isClearlyOutOfScopeCategory(book: BookItem, normalizedTerm: String): Boolean {
        val categories = book.volumeInfo?.categories.orEmpty()
            .map { normalize(it) }
            .filter { it.isNotBlank() }

        if (categories.isEmpty()) return false

        val normalizedTokens = normalizedTerm.split(" ").filter { it.length >= 3 }
        if (normalizedTokens.isEmpty()) return false

        val hasTokenInCategories = normalizedTokens.any { token ->
            categories.any { category -> category.contains(token) }
        }
        if (hasTokenInCategories) return false

        val outOfScopeHints = listOf("juvenile", "children", "infantil", "didatico", "textbook", "comics", "graphic")
        return categories.any { category ->
            outOfScopeHints.any { hint -> category.contains(hint) }
        }
    }

    private fun normalize(value: String?): String {
        return Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .trim()
    }
}

private data class ScoredBook(
    val book: BookItem,
    val score: Int
)

private data class BookScoreResult(
    val score: Int,
    val titleExactMatch: Boolean,
    val titlePartialMatch: Boolean,
    val authorMatch: Boolean,
    val hasImage: Boolean,
    val hasRatings: Boolean,
    val shouldDiscardByCoreMetadata: Boolean,
    val isClearlyOutOfScopeCategory: Boolean
)
