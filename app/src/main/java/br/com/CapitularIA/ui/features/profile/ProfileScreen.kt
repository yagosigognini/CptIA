package br.com.CapitularIA.ui.features.profile

// --- Imports Essenciais ---
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send // Ícone para Pedido Enviado
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check // Ícone para Amigos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.ProfileRatedBook
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.sampleClubsList
import br.com.CapitularIA.data.sampleRatedBooks
import br.com.CapitularIA.data.sampleUser
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.components.ClubsSection
import br.com.CapitularIA.ui.features.home.homeButtonColor
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R
import coil.compose.AsyncImage
import java.util.Locale

// --- TELA "INTELIGENTE" (STATEFUL) ---
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    userId: String?,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClubClick: (BookClub) -> Unit,
    onAddBookClick: () -> Unit,
    onFriendsListClick: () -> Unit,
) {
    LaunchedEffect(key1 = userId) { viewModel.loadUserProfile(userId) }

    // Observa os estados do ViewModel
    val user by viewModel.user.observeAsState()
    val isOwnProfile by viewModel.isOwnProfile.observeAsState(false)
    val clubs by viewModel.clubs.observeAsState(emptyList())
    val bookToRate by viewModel.bookToRate
    val ratedBooks by viewModel.ratedBooks.observeAsState(emptyList())
    val bookToDelete by viewModel.bookToDelete
    val toastMessage by viewModel.toastMessage.observeAsState()

    // ✅ Observa o novo estado de amizade
    val friendshipStatus by viewModel.friendshipStatus.observeAsState(FriendshipStatus.LOADING)

    val friendToRemove by viewModel.friendToRemove

    val context = LocalContext.current

    // Efeito para exibir mensagens Toast
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onToastMessageShown()
        }
    }

    // Diálogo de Avaliação
    if (bookToRate != null) { // ✅ MUDE DE ".let" PARA "if"
        RatingDialog(
            bookTitle = bookToRate?.volumeInfo?.title, // ✅ USE "bookToRate"
            onDismiss = { viewModel.onRatingDialogDismiss() },
            onSubmit = { rating -> viewModel.onRatingSubmitted(rating) }
        )
    }

    // Diálogo de Exclusão
    if (bookToDelete != null) { // ✅ MUDE DE ".let" PARA "if"
        DeleteConfirmationDialog(
            title = "Remover Livro",
            text = "Tem certeza que deseja remover \"${bookToDelete?.title ?: "este livro"}\" da sua estante?", // ✅ USE "bookToDelete"
            onDismiss = { viewModel.cancelDeleteBook() },
            onConfirm = { viewModel.confirmDeleteBook() }
        )
    }

    // ✅ NOVO: Diálogo de Exclusão (para AMIGOS)
    if (friendToRemove != null) { // ✅ MUDE DE ".let" PARA "if"
        DeleteConfirmationDialog(
            title = "Remover Amigo",
            text = "Tem certeza que deseja remover \"${friendToRemove?.name}\" da sua lista de amigos?", // ✅ USE "friendToRemove"
            onDismiss = { viewModel.cancelRemoveFriend() },
            onConfirm = { viewModel.confirmRemoveFriend() }
        )
    }

    // Conteúdo principal
    if (user == null) {
        // Tela de Carregamento
        AppBackground(backgroundResId = R.drawable.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else {
        // Tela de Conteúdo (Stateless)
        ProfileScreenContent(
            user = user!!,
            isOwnProfile = isOwnProfile,
            friendshipStatus = friendshipStatus, // ✅ Passa o estado
            clubs = clubs,
            ratedBooks = ratedBooks,
            onBackClick = onBackClick,
            onEditProfileClick = onEditProfileClick,
            onSettingsClick = onSettingsClick,
            onClubClick = onClubClick,
            onAddBookClick = onAddBookClick,
            onDeleteBookClick = { book -> viewModel.requestDeleteBook(book) },
            onSendFriendRequest = { viewModel.sendFriendRequest() },
            onFriendsListClick = onFriendsListClick,
            onRemoveFriendRequest = { viewModel.requestRemoveFriend() },
        )
    }
}


// --- TELA "BURRA" (STATELESS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    user: User,
    isOwnProfile: Boolean,
    friendshipStatus: FriendshipStatus,
    clubs: List<BookClub>,
    ratedBooks: List<ProfileRatedBook>,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClubClick: (BookClub) -> Unit,
    onAddBookClick: () -> Unit,
    onDeleteBookClick: (ProfileRatedBook) -> Unit,
    onSendFriendRequest: () -> Unit,
    onFriendsListClick: () -> Unit,
    onRemoveFriendRequest: () -> Unit,
) {
    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            containerColor = Color.Transparent, // Fundo transparente
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isOwnProfile) "Meu Perfil" else user.name,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        if (isOwnProfile) {
                            IconButton(onClick = onEditProfileClick) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar Perfil",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, "Configurações", tint = MaterialTheme.colorScheme.onBackground)
                            }

                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { paddingValues ->
            // Conteúdo rolável
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Usa o padding completo do Scaffold (topo e baixo)
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Item 1: O Cabeçalho
                item {
                    ProfileHeader(
                        user = user,
                        isOwnProfile = isOwnProfile,
                        friendshipStatus = friendshipStatus,
                        onSendFriendRequest = onSendFriendRequest,
                        onFriendsListClick = onFriendsListClick,
                        onRemoveFriendRequest = onRemoveFriendRequest,
                    )
                }
                // Item 2: A Estante
                item {
                    ChecklistSection(
                        ratedBooks = ratedBooks,
                        onAddBookClick = onAddBookClick,
                        isOwnProfile = isOwnProfile,
                        onDeleteBookClick = onDeleteBookClick
                    )
                }
                // Item 3: Os Clubes
                item {
                    ClubsSection(
                        title = "Clubes",
                        clubs = clubs,
                        onClubClick = onClubClick
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // Espaço no final
            }
        }
    }
}

// --- COMPONENTES DA TELA ---

@Composable
fun ProfileHeader(
    user: User,
    isOwnProfile: Boolean,
    friendshipStatus: FriendshipStatus,
    onSendFriendRequest: () -> Unit,
    onFriendsListClick: () -> Unit,
    onRemoveFriendRequest: () -> Unit,
) {
    // Coluna principal centralizada
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp) // Padding normal
    ) {
        // Foto de Perfil
        AsyncImage(
            model = user.profilePictureUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "Foto de Perfil",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_launcher_background),
            error = painterResource(id = R.drawable.ic_launcher_background)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Nome
        Text(text = user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Sobre Mim
        if (user.aboutMe.isNotEmpty()) {
            Text(
                text = user.aboutMe,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Row para Contador de Amigos e Botão
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min) // Evita "pulos"
        ) {
            if (isOwnProfile) {
                // SE FOR O PERFIL PRÓPRIO:
                // Contador clicável
                Row(
                    modifier = Modifier.clickable(onClick = onFriendsListClick), // ⬅️ AÇÃO DE CLIQUE AQUI
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Amigos",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${user.friendCount} amigos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


            } else {
                // SE FOR PERFIL DE OUTRA PESSOA:
                // Contador não clicável
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Amigos",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${user.friendCount} amigos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(16.dp)) // Espaço entre contador e botão

                // Botão de Status de Amizade
                when (friendshipStatus) {
                    FriendshipStatus.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    FriendshipStatus.NOT_FRIENDS -> {
                        TextButton(
                            onClick = onSendFriendRequest,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Adicionar Amigo", fontSize = 13.sp)
                        }
                    }
                    FriendshipStatus.REQUEST_SENT -> {
                        TextButton(
                            onClick = { /* TODO: Cancelar pedido? */ },
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Pedido Enviado", fontSize = 13.sp)
                        }
                    }
                    FriendshipStatus.REQUEST_RECEIVED -> {
                        Button(
                            onClick = onFriendsListClick, // ✅ CORREÇÃO: Deve levar para a lista de amigos
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Responder Pedido", fontSize = 13.sp)
                        }
                    }
                    FriendshipStatus.FRIENDS -> {
                        OutlinedButton(
                            onClick = onRemoveFriendRequest, // ✅ LÓGICA DO PONTO 1 (correta)
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Amigos", fontSize = 13.sp)
                        }
                    }
                    FriendshipStatus.SELF -> {
                        // Não acontece (coberto por isOwnProfile), mas necessário para o 'when'
                    }
                }
            }
        } // Fim da Row

        Spacer(modifier = Modifier.height(24.dp)) // Espaço final
    }
}

// Seção da Estante (Checklist)
@Composable
fun ChecklistSection(
    ratedBooks: List<ProfileRatedBook>,
    onAddBookClick: () -> Unit,
    isOwnProfile: Boolean,
    onDeleteBookClick: (ProfileRatedBook) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Minha Estante",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isOwnProfile) {
                Button(
                    onClick = onAddBookClick,
                    colors = ButtonDefaults.buttonColors(containerColor = homeButtonColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Adicionar livro")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (ratedBooks.isEmpty()) {
            Text(
                // ✅ CORREÇÃO: Texto dinâmico
                text = if (isOwnProfile) "Sua estante está vazia. Adicione livros que você já leu!" else "Este usuário ainda não adicionou livros.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = ratedBooks.size, // ✅ Número de itens
                    key = { index -> ratedBooks[index].id } // ✅ Chave única
                ) { index ->
                    val book = ratedBooks[index] // ✅ Obtém o livro pelo índice
                    RatedBookItem(
                        book = book,
                        onDeleteClick = { onDeleteBookClick(book) },
                        isOwnProfile = isOwnProfile
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Item da Estante
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RatedBookItem(
    book: ProfileRatedBook,
    onDeleteClick: () -> Unit,
    isOwnProfile: Boolean, // ✅ CORREÇÃO: Parâmetro adicionado
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(110.dp)
            .combinedClickable(
                onClick = { /* TODO: Ação de clique simples */ },
                // ✅ CORREÇÃO: Só permite deletar se for o perfil do próprio usuário
                onLongClick = if (isOwnProfile) onDeleteClick else null
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = book.coverUrl?.ifEmpty { null },
                contentDescription = book.title ?: "Capa do livro",
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.book_placeholder),
                error = painterResource(id = R.drawable.book_placeholder)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.title ?: "Sem Título",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Nota",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", book.rating),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- PREVIEWS ---
// (Os previews também precisam do novo parâmetro onSearchFriendsClick)

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CapitularIATheme {
        ProfileScreenContent(
            user = sampleUser.copy(friendCount = 10),
            isOwnProfile = true,
            friendshipStatus = FriendshipStatus.SELF,
            clubs = sampleClubsList,
            ratedBooks = sampleRatedBooks,
            onBackClick = {},
            onEditProfileClick = {},
            onSettingsClick = {},
            onClubClick = {},
            onAddBookClick = {},
            onDeleteBookClick = {},
            onSendFriendRequest = {},
            onFriendsListClick = {},
            onRemoveFriendRequest = {}
        )
    }
}

@Preview(showBackground = true, name = "Outro Perfil (Não Amigo)")
@Composable
fun OtherProfileScreenNotFriendPreview() {
    CapitularIATheme {
        ProfileScreenContent(
            user = sampleUser.copy(name="Outro Usuário", friendCount = 5),
            isOwnProfile = false,
            friendshipStatus = FriendshipStatus.NOT_FRIENDS,
            clubs = sampleClubsList.take(1),
            ratedBooks = sampleRatedBooks.take(2),
            onBackClick = {},
            onEditProfileClick = {},
            onSettingsClick = {},
            onClubClick = {},
            onAddBookClick = {},
            onDeleteBookClick = {},
            onSendFriendRequest = {},
            onFriendsListClick = {},
            onRemoveFriendRequest = {}
        )
    }
}

@Preview(showBackground = true, name = "Outro Perfil (Pedido Enviado)")
@Composable
fun OtherProfileScreenRequestSentPreview() {
    CapitularIATheme {
        ProfileScreenContent(
            user = sampleUser.copy(name="Outro Usuário", friendCount = 5),
            isOwnProfile = false,
            friendshipStatus = FriendshipStatus.REQUEST_SENT,
            clubs = sampleClubsList.take(1),
            ratedBooks = sampleRatedBooks.take(2),
            onBackClick = {},
            onEditProfileClick = {},
            onSettingsClick = {},
            onClubClick = {},
            onAddBookClick = {},
            onDeleteBookClick = {},
            onSendFriendRequest = {},
            onFriendsListClick = {},
            onRemoveFriendRequest = {}
        )
    }
}

@Preview
@Composable
fun RatedBookItemPreview() {
    CapitularIATheme {
        RatedBookItem(
            book = ProfileRatedBook(
                title = "O Nome do Vento - Livro Muito Longo Mesmo",
                coverUrl = null,
                rating = 4.5f
            ),
            onDeleteClick = {},
            isOwnProfile = true // ✅ CORREÇÃO: Adicionado ao Preview
        )
    }
}

@Preview(name = "RatedBookItem (Outro Perfil)")
@Composable
fun RatedBookItemOtherProfilePreview() {
    CapitularIATheme {
        RatedBookItem(
            book = ProfileRatedBook(
                title = "O Nome do Vento",
                coverUrl = null,
                rating = 4.5f
            ),
            onDeleteClick = {},
            isOwnProfile = false // ✅ CORREÇÃO: Testando sem permissão de delete
        )
    }
}