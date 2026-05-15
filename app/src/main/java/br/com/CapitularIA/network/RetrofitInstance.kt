package br.com.CapitularIA.network

import br.com.CapitularIA.BuildConfig // Import para acessar a chave da API
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient // Import para OkHttp
import okhttp3.logging.HttpLoggingInterceptor // Import para Logging

object RetrofitInstance {

    // URL base da API do Google Books
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    // Cria um interceptor para logar as chamadas de rede (ótimo para debug)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) { // Só loga em modo debug
            HttpLoggingInterceptor.Level.BODY // Loga tudo: url, headers, corpo da requisição/resposta
        } else {
            HttpLoggingInterceptor.Level.NONE // Não loga nada em release
        }
    }

    // Cria um cliente OkHttp que usa o interceptor de log
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Cria a instância do Retrofit
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Usa o cliente OkHttp configurado
            .addConverterFactory(GsonConverterFactory.create()) // Usa Gson para converter JSON
            .build()
    }

    // Cria (preguiçosamente) uma instância da nossa interface da API
    val api: GoogleBooksApiService by lazy {
        retrofit.create(GoogleBooksApiService::class.java)
    }

    // Acessa a chave da API de forma segura a partir do BuildConfig
    val apiKey: String = BuildConfig.GOOGLE_BOOKS_API_KEY
}