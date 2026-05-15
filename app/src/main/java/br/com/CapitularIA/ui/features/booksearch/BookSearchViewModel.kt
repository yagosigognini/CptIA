package br.com.CapitularIA.ui.features.booksearch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.BookItem // Importa o modelo do livro
import br.com.CapitularIA.data.getBestAvailableImageUrl
import br.com.CapitularIA.network.RetrofitInstance // Importa nossa instância do Retrofit
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
        if (query.isBlank()) {
            _searchState.value = BookSearchState.Idle // Limpa se a busca for vazia
            return
        }

        viewModelScope.launch {
            _searchState.value = BookSearchState.Loading // Avisa a UI que está carregando
            try {
                // Chama a função da nossa interface Retrofit
                val response = apiService.searchBooks(query = query, apiKey = apiKey)

                if (response.isSuccessful) {
                    // Sucesso: Pega a lista de livros (items) da resposta
                    val books = response.body()?.items ?: emptyList()

                    // ⬇️ --- LOGS ADICIONADOS --- ⬇️
                    Log.d("BookSearchVM", "Recebidos ${books.size} livros para '$query'.")
                    books.take(3).forEachIndexed { index, item -> // Loga os 3 primeiros
                        Log.d("BookSearchVM", "Livro $index - Título: ${item.volumeInfo?.title}")
                        // Loga o objeto imageLinks inteiro para ver todas as URLs
                        Log.d("BookSearchVM", "Livro $index - ImageLinks Object: ${item.volumeInfo?.imageLinks}")
                        // Loga a URL específica que sua função utilitária escolheria
                        Log.d("BookSearchVM", "Livro $index - Best URL: ${item.volumeInfo?.imageLinks.getBestAvailableImageUrl()}")
                    }
                    // ⬆️ --- FIM DOS LOGS --- ⬆️

                    // Atualiza o estado da UI DEPOIS de logar
                    _searchState.value = BookSearchState.Success(books)
                    Log.d("BookSearchVM", "Busca bem-sucedida (estado atualizado) para '$query'") // Log original movido para depois da atualização do estado

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
}