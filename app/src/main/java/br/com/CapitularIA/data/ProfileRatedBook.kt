package br.com.CapitularIA.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId // ⬅️ Importar
import com.google.firebase.firestore.ServerTimestamp

data class ProfileRatedBook(
    @DocumentId val id: String = "", // ⬅️ ADICIONAR ESTA LINHA (ID do Documento Firestore)
    val googleBookId: String = "",
    val title: String? = null,
    val authors: List<String>? = null,
    val coverUrl: String? = null,
    val readingStatus: String = ReadingStatus.WANT_TO_READ.name,
    val rating: Float = 0f,
    @ServerTimestamp
    val finishedAt: Timestamp? = null,
    @ServerTimestamp
    val ratedAt: Timestamp? = null
) {
    // Construtor vazio necessário para o Firestore
    constructor() : this("", "", null, null, null, ReadingStatus.WANT_TO_READ.name, 0f, null, null)
}
