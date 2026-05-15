package br.com.CapitularIA.ui.features.booksearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.data.BookItem
import br.com.CapitularIA.data.VolumeInfo
import br.com.CapitularIA.data.getBestAvailableImageUrl
import br.com.CapitularIA.ui.components.AppBackground // Import correto
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import br.com.CapitularIA.data.ImageLinks

@Composable
fun BookSearchScreen(
    onBookSelected: (BookItem) -> Unit,
    onBackClick: () -> Unit,
    viewModel: BookSearchViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun performSearch() {
        viewModel.searchBooks(searchQuery)
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun clearSearchQuery() {
        searchQuery = ""
        viewModel.clearSearch()
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    BookSearchScreenContent(
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        searchState = searchState,
        onSearchClick = { performSearch() },
        onClearClick = { clearSearchQuery() },
        onBookClick = { bookItem ->
            onBookSelected(bookItem)
        },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchScreenContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchState: BookSearchState,
    onSearchClick: () -> Unit,
    onClearClick: () -> Unit,
    onBookClick: (BookItem) -> Unit,
    onBackClick: () -> Unit
) {
    // ✅ --- CORREÇÃO AQUI ---
    // Voltando a usar backgroundResId com a imagem correta para esta tela
    AppBackground(backgroundResId = R.drawable.background) { // Use R.drawable.background ou R.drawable.app_background
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Buscar Livro") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Campo de busca
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    label = { Text("Digite o título ou autor") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearClick) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchClick() })
                )

                // Conteúdo central (Loading, Erro, Resultados ou Idle)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (searchState) {
                        is BookSearchState.Idle -> {
                            Text("Digite algo para buscar livros.")
                        }
                        is BookSearchState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is BookSearchState.Error -> {
                            Text("Erro: ${searchState.message}", color = MaterialTheme.colorScheme.error)
                        }
                        is BookSearchState.Success -> {
                            if (searchState.books.isEmpty()) {
                                Text("Nenhum livro encontrado para \"$searchQuery\".")
                            } else {
                                // Lista de resultados
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(searchState.books) { bookItem ->
                                        BookItemRow(bookItem = bookItem, onClick = { onBookClick(bookItem) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookItemRow(
    bookItem: BookItem,
    onClick: () -> Unit
) {
    val volumeInfo = bookItem.volumeInfo ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = volumeInfo.imageLinks.getBestAvailableImageUrl(),
                contentDescription = "Capa de ${volumeInfo.title}",
                modifier = Modifier
                    .height(80.dp)
                    .width(60.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_launcher_background),
                error = painterResource(id = R.drawable.ic_launcher_background)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volumeInfo.title ?: "Título desconhecido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, // ✅ Referência corrigida com import
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = volumeInfo.authors?.joinToString(", ") ?: "Autor desconhecido",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Editora: ${volumeInfo.publisher ?: "-"} | Ano: ${volumeInfo.publishedDate?.substringBefore("-") ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- PREVIEWS ---
// ... (Previews não precisam mudar) ...
@Preview(showBackground = true)
@Composable
fun BookSearchScreenContentIdlePreview() {
    CapitularIATheme {
        BookSearchScreenContent(
            searchQuery = "",
            onSearchQueryChange = {},
            searchState = BookSearchState.Idle,
            onSearchClick = {},
            onClearClick = {},
            onBookClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookSearchScreenContentLoadingPreview() {
    CapitularIATheme {
        BookSearchScreenContent(
            searchQuery = "Kotlin",
            onSearchQueryChange = {},
            searchState = BookSearchState.Loading,
            onSearchClick = {},
            onClearClick = {},
            onBookClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookSearchScreenContentSuccessPreview() {
    val sampleBooks = listOf(
        BookItem("1", VolumeInfo("Kotlin em Ação", null, listOf("Dmitry Jemerov", "Svetlana Isakova"), "Novatec", "2017", "Livro sobre Kotlin...", 400, null, ImageLinks(null, "https://via.placeholder.com/150", null, null, null, null), 4.5, 100, null)),
        BookItem("2", VolumeInfo("Android com Kotlin", "Desenvolvimento Moderno", listOf("Marcos Placona"), "Casa do Código", "2020", "Aprenda Android...", 300, null, ImageLinks(null, "https://via.placeholder.com/150", null, null, null, null), 4.0, 50, null))
    )
    CapitularIATheme {
        BookSearchScreenContent(
            searchQuery = "Kotlin",
            onSearchQueryChange = {},
            searchState = BookSearchState.Success(sampleBooks),
            onSearchClick = {},
            onClearClick = {},
            onBookClick = {},
            onBackClick = {}
        )
    }
}