package br.com.CapitularIA.ui.features.searchuser

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SearchUserViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // StateFlow para expor o estado da busca para a UI
    private val _searchState = MutableStateFlow<SearchUserState>(SearchUserState.Idle)
    val searchState: StateFlow<SearchUserState> = _searchState

    /**
     * Busca usuários no Firestore pelo nome.
     * @param query O nome (ou parte do nome) a ser buscado.
     */
    fun searchUsers(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            _searchState.value = SearchUserState.Idle
            return
        }

        // ✅ Converte a query do usuário para minúsculas
        val lowerCaseQuery = trimmedQuery.lowercase(Locale.ROOT)

        _searchState.value = SearchUserState.Loading
        val currentUserId = auth.currentUser?.uid

        viewModelScope.launch {
            try {
                // ✅ Query agora usa o campo 'name_lowercase'
                val result = db.collection("users")
                    .whereGreaterThanOrEqualTo("name_lowercase", lowerCaseQuery)
                    .whereLessThanOrEqualTo("name_lowercase", lowerCaseQuery + '\uf8ff')
                    .orderBy("name_lowercase") // ✅ Ordena pelo novo campo
                    .limit(20)
                    .get()
                    .await()

                val users = result.toObjects(User::class.java).filter { it.uid != currentUserId }

                if (users.isEmpty()) {
                    _searchState.value = SearchUserState.NoResults
                } else {
                    _searchState.value = SearchUserState.Success(users)
                }
                Log.d(
                    "SearchUserVM",
                    "Busca por '$lowerCaseQuery' retornou ${users.size} usuários."
                )

            } catch (e: Exception) {
                Log.e("SearchUserVM", "Erro ao buscar usuários por '$lowerCaseQuery'", e)
                _searchState.value = SearchUserState.Error("Erro ao buscar usuários: ${e.message}")
            }
        }
    }

    /** Reseta o estado da busca para Idle. */
    fun clearSearch() {
        _searchState.value = SearchUserState.Idle
    }
}