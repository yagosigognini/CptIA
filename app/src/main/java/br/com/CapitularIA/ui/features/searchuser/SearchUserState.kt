package br.com.CapitularIA.ui.features.searchuser // Ou onde seu ViewModel/Screen ficarão

import br.com.CapitularIA.data.User // Importe sua classe User

// Define os possíveis estados da tela de busca de usuários
sealed class SearchUserState {
    data object Idle : SearchUserState() // Estado inicial, nada buscado ainda
    data object Loading : SearchUserState() // Buscando no Firestore
    data class Success(val users: List<User>) : SearchUserState() // Busca concluída com sucesso, lista de usuários encontrada
    data object NoResults : SearchUserState() // Busca concluída, mas nenhum usuário encontrado
    data class Error(val message: String) : SearchUserState() // Ocorreu um erro
}