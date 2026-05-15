package br.com.CapitularIA.data

import android.os.Parcelable         // ⬇️ NOVO: Importar (Boa prática, caso precise passá-lo)
import kotlinx.parcelize.Parcelize     // ⬇️ NOVO: Importar

@Parcelize // ⬇️ NOVO: Anotar (Boa prática)
data class IndicatedBook(
    val googleBookId: String = "", // ⬅️ CAMPO ADICIONADO
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val coverUrl: String = ""
) : Parcelable { // ⬇️ NOVO: Implementar (Boa prática)

    // Construtor vazio necessário para o Firestore ler ("toObject()")
    constructor() : this("", "", "", "", "")
}