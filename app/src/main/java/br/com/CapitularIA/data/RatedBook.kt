package br.com.CapitularIA.data

data class RatedBook(
    val book: Book? = null, // Usar 'Book?' permite que o valor padrão seja null
    val rating: Int = 0
)