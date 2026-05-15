package br.com.CapitularIA.ui.features.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.sampleClubsList
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.components.ClubItemCard
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R

@Composable
fun SearchClubScreen(
    viewModel: SearchClubViewModel = viewModel(),
    onBackClick: () -> Unit,
    onClubClick: (BookClub) -> Unit,
    detailsViewModel: ClubDetailsViewModel = viewModel() // Este VM já estava aqui
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClubId by remember { mutableStateOf<String?>(null) }

    val allClubs by viewModel.clubs.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    // A filtragem de clubes públicos continua funcionando
    val filteredClubs = allClubs.filter { it.name.contains(searchQuery, ignoreCase = true) }

    // O diálogo para clubes públicos continua funcionando
    selectedClubId?.let { id ->
        ClubDetailsDialog(
            clubId = id,
            viewModel = detailsViewModel,
            onDismiss = { selectedClubId = null },
            onJoinClick = { detailsViewModel.joinClub() }
        )
    }

    SearchClubScreenContent(
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        clubs = filteredClubs,
        isLoading = isLoading,
        onBackClick = onBackClick,
        onClubClick = { club -> selectedClubId = club.id },
        // ✅ Ação de clique para o novo botão de código
        onCodeRequestClick = { code ->
            detailsViewModel.requestToJoinByCode(code)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchClubScreenContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    clubs: List<BookClub>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onClubClick: (BookClub) -> Unit,
    onCodeRequestClick: (String) -> Unit // ✅ PARÂMETRO ADICIONADO
) {
    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Procurar Clube") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp), // Padding ajustado
                    placeholder = { Text("Insira o nome ou o código de acesso") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ícone de busca") },
                    singleLine = true
                )

                // ✅ --- NOVO BOTÃO PARA SOLICITAR COM CÓDIGO ---
                Button(
                    onClick = { onCodeRequestClick(searchQuery) },
                    enabled = searchQuery.isNotBlank(), // Habilitado apenas se houver texto
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("SOLICITAR COM CÓDIGO")
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                // --- Fim da adição ---

                Text(
                    "Clubes públicos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = clubs.size,
                            key = { index -> clubs[index].id ?: index }
                        ) { index ->
                            val club = clubs[index]
                            ClubItemCard(
                                club = club,
                                onClubClick = onClubClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchClubScreenPreview() {
    CapitularIATheme {
        SearchClubScreenContent(
            searchQuery = "",
            onSearchQueryChange = {},
            clubs = sampleClubsList,
            isLoading = false,
            onBackClick = {},
            onClubClick = {},
            onCodeRequestClick = {} // ✅ ADICIONADO AO PREVIEW
        )
    }
}