package br.com.CapitularIA.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "", // Para exibir o nome sem buscar o usuário toda hora
    val senderPhotoUrl: String = "", // Para exibir a foto sem buscar o usuário toda hora

    val type: String = "USER", // Tipos: "USER" ou "INDICATION"

    // ✅ NOVO: Armazena os dados do livro, se a mensagem for do tipo "INDICATION"
    val indicatedBook: IndicatedBook? = null,

    // Anotação especial que diz ao Firestore para preencher este campo com a hora do servidor
    @ServerTimestamp
    val timestamp: Date? = null
)