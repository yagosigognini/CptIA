package br.com.CapitularIA.network

import br.com.CapitularIA.data.GoogleBooksResponse
import retrofit2.Response // Importante: Usar o do Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {

    // Define o endpoint da API que queremos acessar
    // O caminho completo será: https://www.googleapis.com/books/v1/volumes
    @GET("volumes")
    suspend fun searchBooks(
        // Parâmetro 'q': O termo de busca (ex: "Kotlin Android")
        @Query("q") query: String,

        // Parâmetro 'maxResults': Quantos resultados queremos (ex: 10)
        @Query("maxResults") maxResults: Int = 10,

        // Parâmetro 'langRestrict': Restringe resultados por idioma (ex: "pt")
        @Query("langRestrict") langRestrict: String? = null,

        // Parâmetro 'printType': Tipo de material (ex: "books", "magazines", "all")
        @Query("printType") printType: String? = null,

        // Parâmetro 'orderBy': Ordenação dos resultados (ex: "relevance", "newest")
        @Query("orderBy") orderBy: String? = null,

        // Parâmetro 'filter': Filtros de disponibilidade/tipo (ex: "ebooks", "free-ebooks")
        @Query("filter") filter: String? = null,

        // Parâmetro 'projection': Quantidade de dados retornados (ex: "full", "lite")
        @Query("projection") projection: String? = null,

        // Parâmetro 'key': Sua chave de API do Google Cloud Console
        @Query("key") apiKey: String
    ): Response<GoogleBooksResponse> // Retorna a resposta completa, incluindo o objeto que definimos
}
