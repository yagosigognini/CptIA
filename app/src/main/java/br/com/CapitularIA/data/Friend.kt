package br.com.CapitularIA.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Representa um amigo na subcoleção /users/{userId}/friends/{friendId}
data class Friend(
    @DocumentId val uid: String = "", // O ID do documento será o UID do amigo
    val name: String = "",
    val profilePictureUrl: String = "",
    @ServerTimestamp
    val addedAt: Date? = null // Data que a amizade foi adicionada
) {
    // Construtor vazio para o Firestore
    constructor() : this("", "", "", null)
}