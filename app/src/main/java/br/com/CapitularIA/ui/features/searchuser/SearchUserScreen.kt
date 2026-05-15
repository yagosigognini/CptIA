package br.com.CapitularIA.ui.features.searchuser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.data.User // Importa sua classe User
import br.com.CapitularIA.data.sampleUser // Para Preview
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R
import coil.compose.AsyncImage

// --- TELA "INTELIGENTE" (STATEFUL) ---
@Composable
fun SearchUserScreen(
    viewModel: SearchUserViewModel = viewModel(), // Obtém o ViewModel
    onUserClick: (String) -> Unit, // Callback quando um usuário é clicado (passa o userId)
    onBackClick: () -> Unit // Callback para voltar
) {
    // Estado local para o texto da busca
    var searchQuery by remember { mutableStateOf("") }
    // Observa o estado da busca vindo do ViewModel
    val searchState by viewModel.searchState.collectAsState()
    // Controladores para teclado e foco
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Função para executar a busca
    fun performSearch() {
        viewModel.searchUsers(searchQuery) // Chama o ViewModel
        keyboardController?.hide() // Esconde o teclado
        focusManager.clearFocus() // Remove o foco do campo de texto
    }

    // Função para limpar a busca
    fun clearSearch() {
        searchQuery = "" // Limpa o texto
        viewModel.clearSearch() // Reseta o estado no ViewModel
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    // Passa os estados e callbacks para a tela "burra"
    SearchUserScreenContent(
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it }, // Atualiza o texto local
        searchState = searchState,
        onSearchClick = { performSearch() }, // Ação de buscar
        onClearClick = { clearSearch() }, // Ação de limpar
        onUserClick = onUserClick, // Passa o callback de clique no usuário
        onBackClick = onBackClick // Passa o callback de voltar
    )
}

// --- TELA "BURRA" (STATELESS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreenContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchState: SearchUserState,
    onSearchClick: () -> Unit,
    onClearClick: () -> Unit,
    onUserClick: (String) -> Unit, // Recebe o callback com userId
    onBackClick: () -> Unit
) {
    AppBackground(backgroundResId = R.drawable.background) { // Fundo padrão
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Buscar Amigos") }, // Título da tela
                    navigationIcon = { // Botão de voltar
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets.statusBars // Ajuste para status bar
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Padding da Scaffold
                    .padding(horizontal = 16.dp) // Padding lateral
            ) {
                // Campo de busca
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    label = { Text("Digite o nome do usuário") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { // Ícone para limpar o campo
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearClick) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), // Botão "Buscar" no teclado
                    keyboardActions = KeyboardActions(onSearch = { onSearchClick() }) // Ação ao clicar no botão do teclado
                )

                // Área de conteúdo (Resultados, Loading, Erro, etc.)
                Box(
                    modifier = Modifier.fillMaxSize(), // Ocupa o restante do espaço
                    contentAlignment = Alignment.Center // Centraliza conteúdo (Loading, mensagens)
                ) {
                    when (searchState) {
                        is SearchUserState.Idle -> { // Estado inicial
                            Text("Digite um nome para encontrar usuários.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is SearchUserState.Loading -> { // Carregando
                            CircularProgressIndicator()
                        }
                        is SearchUserState.Error -> { // Erro
                            Text("Erro: ${searchState.message}", color = MaterialTheme.colorScheme.error)
                        }
                        is SearchUserState.NoResults -> { // Nenhum resultado
                            Text("Nenhum usuário encontrado para \"$searchQuery\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is SearchUserState.Success -> { // Sucesso, mostra a lista
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                // ✅ CORREÇÃO AQUI - Usando itemsIndexed
                                itemsIndexed(searchState.users) { index, user ->
                                    UserResultItem(
                                        user = user,
                                        onClick = { onUserClick(user.uid) } // ✅ 'uid' será corrigido abaixo
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ITEM DA LISTA DE RESULTADOS ---
@Composable
fun UserResultItem(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Torna o Card clicável
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // Sombra leve
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto do usuário
            AsyncImage(
                model = user.profilePictureUrl.ifEmpty { R.drawable.ic_launcher_background }, // Placeholder
                contentDescription = "Foto de ${user.name}",
                modifier = Modifier
                    .size(48.dp) // Tamanho da foto
                    .clip(CircleShape), // Formato circular
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_launcher_background),
                error = painterResource(id = R.drawable.ic_launcher_background)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Nome do usuário
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, // Destaque leve
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Poderia adicionar mais informações aqui se quisesse (ex: user.aboutMe)
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
fun SearchUserScreenContentIdlePreview() {
    CapitularIATheme {
        SearchUserScreenContent(
            searchQuery = "",
            onSearchQueryChange = {},
            searchState = SearchUserState.Idle,
            onSearchClick = {},
            onClearClick = {},
            onUserClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchUserScreenContentLoadingPreview() {
    CapitularIATheme {
        SearchUserScreenContent(
            searchQuery = "Buscan",
            onSearchQueryChange = {},
            searchState = SearchUserState.Loading,
            onSearchClick = {},
            onClearClick = {},
            onUserClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchUserScreenContentNoResultsPreview() {
    CapitularIATheme {
        SearchUserScreenContent(
            searchQuery = "xyz123",
            onSearchQueryChange = {},
            searchState = SearchUserState.NoResults,
            onSearchClick = {},
            onClearClick = {},
            onUserClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchUserScreenContentSuccessPreview() {
    val sampleUsers = listOf(
        sampleUser.copy(uid = "1", name = "Usuário Exemplo 1"),
        sampleUser.copy(uid = "2", name = "Outro Exemplo"),
        sampleUser.copy(uid = "3", name = "Terceiro da Silva")
    )
    CapitularIATheme {
        SearchUserScreenContent(
            searchQuery = "Exemplo",
            onSearchQueryChange = {},
            searchState = SearchUserState.Success(sampleUsers),
            onSearchClick = {},
            onClearClick = {},
            onUserClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UserResultItemPreview() {
    CapitularIATheme {
        UserResultItem(user = sampleUser.copy(name = "Nome Bem Longo Para Testar o Layout"), onClick = {})
    }
}