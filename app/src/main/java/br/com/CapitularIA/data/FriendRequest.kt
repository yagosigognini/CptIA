package br.com.CapitularIA.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FriendRequest(
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",

    val receiverId: String = "",
    val receiverName: String = "", // Campo necessário
    val receiverPhotoUrl: String = "", // Campo necessário

    val status: String = "pending",
    @ServerTimestamp
    val timestamp: Date? = null
) {
    // Construtor vazio para o Firestore (atualizado com 6 campos de string)
    constructor() : this("", "", "", "", "", "", "pending", null)
}