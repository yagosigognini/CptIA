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

        // Parâmetro 'key': Sua chave de API do Google Cloud Console
        @Query("key") apiKey: String
    ): Response<GoogleBooksResponse> // Retorna a resposta completa, incluindo o objeto que definimos
}