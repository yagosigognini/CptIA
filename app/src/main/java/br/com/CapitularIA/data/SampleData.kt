package br.com.CapitularIA.data

import br.com.CapitularIA.R
import com.google.firebase.Timestamp // ⬇️ NOVO: Importar Timestamp

// --- DADOS DE EXEMPLO PARA O USUÁRIO ---
val sampleUser = User(
    uid = "123",
    name = "Clarice Lispector",
    email = "clarice@exemplo.com",
    aboutMe = "Escritora e jornalista, considerada uma das mais importantes do século XX.",
    profilePictureUrl = "https://picsum.photos/seed/clarice/200",
    friendCount = 67
)

// --- DADOS DE EXEMPLO PARA A HOME SCREEN ---

val sampleQuote = Quote(
    text = "\"Tu te tornas eternamente responsável por aquilo que cativas.\"",
    source = "O Pequeno Príncipe – Antoine de Saint-Exupéry"
)

// --- DADOS DE EXEMPLO PARA OS CLUBES ---
val sampleClubsList = listOf(
    BookClub(
        id = "1",
        name = "Clássicos da Meia-Noite",
        imageUrl = "https://picsum.photos/seed/101/400/200",
        members = List(7) { "" }, // Usando o nome correto "members"
        maxMembers = 15
    ),
    BookClub(
        id = "2",
        name = "Terror & Suspense",
        imageUrl = "https://picsum.photos/seed/102/400/200",
        members = List(12) { "" },
        maxMembers = 20
    ),
    BookClub(
        id = "3",
        name = "Aventura Fantástica",
        imageUrl = "https://picsum.photos/seed/103/400/200",
        members = List(5) { "" },
        maxMembers = 10
    )
)

// ⬇️ --- NOVO: DADOS DE EXEMPLO PARA OS LIVROS AVALIADOS --- ⬇️
val sampleRatedBooks = listOf(
    ProfileRatedBook(
        googleBookId="1",
        title="Livro de Exemplo 1",
        authors=listOf("Autor A"),
        coverUrl = null, // Deixe null para testar o placeholder no preview
        rating = 4.0f,
        ratedAt = Timestamp.now() // Use Timestamp.now() para o preview
    ),
    ProfileRatedBook(
        googleBookId="2",
        title="Outro Livro Com Título Bem Longo Para Testar",
        authors=listOf("Autor B", "Coautor"),
        coverUrl = null,
        rating = 3.5f,
        ratedAt = Timestamp.now()
    ),
    ProfileRatedBook(
        googleBookId="3",
        title="Livro 3",
        authors=listOf("Autor C"),
        coverUrl = null,
        rating = 5.0f,
        ratedAt = Timestamp.now()
    )
)
// ⬆️ --- FIM DOS DADOS DE EXEMPLO --- ⬆️