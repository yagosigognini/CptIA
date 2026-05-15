package br.com.CapitularIA.data

import android.os.Parcelable         // ⬅️ IMPORTAR
import kotlinx.parcelize.Parcelize     // ⬅️ IMPORTAR

// Estrutura principal da resposta da API (uma lista de itens)
// Não precisa ser Parcelable, pois não é passada entre telas.
data class GoogleBooksResponse(
    val items: List<BookItem>? = null // A lista de livros encontrados
)

// Cada item na lista de resultados
@Parcelize // ⬅️ ADICIONAR
data class BookItem(
    val id: String?, // ID único do Google para o livro
    val volumeInfo: VolumeInfo? // Informações detalhadas do livro
) : Parcelable // ⬅️ IMPLEMENTAR

// Detalhes do volume (livro)
@Parcelize // ⬅️ ADICIONAR
data class VolumeInfo(
    val title: String?,
    val subtitle: String?,
    val authors: List<String>?,
    val publisher: String?,
    val publishedDate: String?,
    val description: String?,
    val pageCount: Int?,
    val categories: List<String>?,
    val imageLinks: ImageLinks?, // Links para as capas
    val averageRating: Double?,
    val ratingsCount: Int?,
    val infoLink: String? // Link para a página do livro no Google Books
) : Parcelable // ⬅️ IMPLEMENTAR

// Links das imagens de capa (vários tamanhos)
@Parcelize // ⬅️ ADICIONAR
data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?,
    val small: String?,
    val medium: String?,
    val large: String?,
    val extraLarge: String?
) : Parcelable // ⬅️ IMPLEMENTAR

// Função utilitária para pegar a melhor URL de imagem disponível
fun ImageLinks?.getBestAvailableImageUrl(): String? {
    // Esta função não muda
    return this?.thumbnail ?: this?.smallThumbnail ?: this?.small ?: this?.medium ?: this?.large ?: this?.extraLarge
}