package br.com.CapitularIA.ui.features.club

// --- Imports Essenciais ---
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.CapitularIA.R
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.IndicatedBook
import br.com.CapitularIA.data.Message
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.ValidatedRecommendation
import br.com.CapitularIA.data.sampleClubsList
import br.com.CapitularIA.data.sampleUser
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.CapitularIATheme
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- TELA "INTELIGENTE" (STATEFUL) ---
@Composable
fun ClubScreen(
    clubId: String,
    viewModel: ClubViewModel, // Recebe o ViewModel injetado pela Navegação
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onAdminClick: () -> Unit,
    onSearchBookClick: () -> Unit // Recebe o lambda da Navegação para buscar livro
) {
    // Inicia o carregamento quando o clubId muda (ou na primeira vez)
    LaunchedEffect(clubId) {
        viewModel.loadClubAndMessages(clubId)
    }

    // Observa os estados do ViewModel
    val club by viewModel.club.observeAsState()
    val messages by viewModel.messages.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(true)
    val accessDenied by viewModel.accessDenied.observeAsState(false)
    val sortedUser by viewModel.sortedUser.observeAsState(null)
    val bookPendingIndication by viewModel.bookPendingIndication // Observa o livro pendente vindo da busca
    val recommendations by viewModel.recommendations.observeAsState(emptyList())
    val isLoadingRecommendations by viewModel.isLoadingRecommendations.observeAsState(false)

    var inputText by remember { mutableStateOf("") } // Estado local para o campo de mensagem
    var isQuickMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedQuickAction by rememberSaveable { mutableStateOf<QuickAction?>(null) }
    var recommendationInput by rememberSaveable { mutableStateOf("") }

    // Mostra o diálogo de confirmação se houver um livro pendente (retornado da busca)
    bookPendingIndication?.let { book ->
        IndicationConfirmationDialog(
            bookTitle = book.volumeInfo?.title, // Passa o título
            // Usa os dias configurados no clube ou 15 como padrão inicial
            initialCycleDays = club?.readingCycleDays?.toString() ?: "15",
            onDismiss = { viewModel.cancelBookIndication() }, // Ação de cancelar
            onConfirm = { daysString -> viewModel.confirmBookIndication(daysString) } // Ação de confirmar
        )
    }

    // Passa os estados e callbacks para a tela "burra" (Content)
    ClubScreenContent(
        club = club,
        messages = messages,
        sortedUser = sortedUser,
        inputText = inputText,
        onInputChange = { inputText = it },
        onSendMessage = {
            if (inputText.isNotBlank()) {
                viewModel.sendMessage(inputText, clubId)
                inputText = "" // Limpa o campo após enviar
            }
        },
        onSortearClick = { viewModel.drawUserForCycle() }, // Callback para sortear
        onSearchBookClick = onSearchBookClick, // Passa o callback de busca
        isLoading = isLoading,
        accessDenied = accessDenied,
        onBackClick = onBackClick,
        onProfileClick = onProfileClick,
        onAdminClick = onAdminClick,
        isQuickMenuExpanded = isQuickMenuExpanded,
        onToggleQuickMenu = { isQuickMenuExpanded = !isQuickMenuExpanded },
        onQuickActionClick = { action ->
            selectedQuickAction = action
            isQuickMenuExpanded = false
        }
    )

    selectedQuickAction?.let { action ->
        QuickActionDialog(
            action = action,
            club = club,
            recommendations = recommendations,
            isLoadingRecommendations = isLoadingRecommendations,
            recommendationInput = recommendationInput,
            onRecommendationInputChange = { recommendationInput = it },
            onRequestRecommendations = {
                val chatContext = messages
                    .asReversed()
                    .mapNotNull { message -> message.text?.trim() }
                    .filter { it.isNotBlank() }
                    .take(6)
                    .asReversed()
                    .joinToString("\n")

                val recommendationPrompt = when {
                    recommendationInput.isNotBlank() -> recommendationInput
                    inputText.isNotBlank() -> inputText
                    chatContext.isNotBlank() -> "Contexto recente do chat:\n$chatContext"
                    else -> "Recomende livros para o perfil atual do clube."
                }
                viewModel.requestBookRecommendations(recommendationPrompt)
            },
            onDismiss = { selectedQuickAction = null }
        )
    }
}

// --- TELA "BURRA" (STATELESS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubScreenContent(
    club: BookClub?,
    messages: List<Message>,
    sortedUser: User?,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSortearClick: () -> Unit,
    onSearchBookClick: () -> Unit, // Recebe o lambda de busca
    isLoading: Boolean,
    accessDenied: Boolean,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onAdminClick: () -> Unit,
    isQuickMenuExpanded: Boolean,
    onToggleQuickMenu: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit
) {
    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            bottomBar = {
                // Mostra o campo de input apenas se o usuário for membro
                if (!accessDenied) {
                    MessageInput(
                        text = inputText, // Passa o texto atual
                        onTextChange = onInputChange, // Passa a função para mudar o texto
                        onSendClick = onSendMessage, // Passa a função de enviar
                        isQuickMenuExpanded = isQuickMenuExpanded,
                        onToggleQuickMenu = onToggleQuickMenu,
                        onQuickActionClick = onQuickActionClick
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()) // Ajusta padding inferior
            ) {
                // Cabeçalho com imagem e nome do clube
                ClubHeader(
                    club = club, // Passa os dados do clube
                    onBackClick = onBackClick, // Passa a função de voltar
                    onAdminClick = onAdminClick // Passa a função de admin
                )


                if (club != null) {
                    ClubPreferencesSection(club = club)
                }

                // Painel de Ações (Sorteio/Indicação) - Mostrado apenas se for membro
                if (club != null && !accessDenied) {
                    ActionPanel(
                        club = club, // Passa os dados do clube
                        sortedUser = sortedUser, // Passa o usuário sorteado
                        onSortearClick = onSortearClick, // Passa a função de sortear
                        onSearchBookClick = onSearchBookClick // Passa a função de busca
                    )
                }

                // Corpo principal: Loading, Acesso Negado ou Lista de Mensagens
                when {
                    isLoading -> { // Mostra indicador de carregamento
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    accessDenied -> { // Mostra mensagem de acesso negado
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                "Você não é membro deste clube.",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error

                            )
                        }
                    }
                    else -> { // Mostra a lista de mensagens
                        MessageList(
                            messages = messages, // Passa a lista de mensagens
                            onProfileClick = onProfileClick, // Passa a função de clique no perfil
                            modifier = Modifier.weight(1f) // Ocupa o espaço restante
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENTES DA TELA ---

@Composable
fun ActionPanel(
    club: BookClub,
    sortedUser: User?,
    onSortearClick: () -> Unit,
    onSearchBookClick: () -> Unit // Recebe o lambda de busca
) {
    val currentUserId = Firebase.auth.currentUser?.uid
    val isAdmin = currentUserId == club.adminId
    val isSortedUser = currentUserId == club.currentUserForCycleId

    val sortedUserId = club.currentUserForCycleId
    val indicatedBook = club.indicatedBook

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Ajuste de padding
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Estado 1: Ninguém sorteado
            if (sortedUserId == null) {
                if (isAdmin) {
                    Text("Ninguém foi sorteado para indicar o próximo livro.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onSortearClick) { Text("Sortear usuário") }
                } else {
                    Text("Aguardando o admin sortear o próximo usuário.")
                }
            }
            // Estado 2: Sorteado, mas sem livro indicado
            else if (indicatedBook == null) {
                if (isSortedUser) { // Se for a vez do usuário atual
                    Text("Sua vez de indicar o próximo livro!", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    // Botão para navegar para a tela de busca
                    Button(onClick = onSearchBookClick) {
                        Icon(Icons.Default.Search, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Buscar e Indicar Livro")
                    }
                } else { // Se for a vez de outro usuário
                    val sortedUserName = sortedUser?.name ?: "o usuário sorteado"
                    Text("Aguardando $sortedUserName indicar um livro.", textAlign = TextAlign.Center)
                }
            }
            // Estado 3: Livro já indicado
            else {
                val now = System.currentTimeMillis()
                val endDate = club.cycleEndDate ?: now // Usa data atual se endDate for nulo
                // Calcula dias restantes (garante que não seja negativo)
                val daysRemaining = if (endDate < now) 0 else TimeUnit.MILLISECONDS.toDays(endDate - now)

                // Texto informativo sobre o ciclo
                val textToShow = when {
                    daysRemaining > 1 -> "Faltam $daysRemaining dias para o fim do ciclo"
                    daysRemaining == 1L -> "Último dia do ciclo!"
                    else -> "Ciclo de leitura encerrado!"
                }

                Text(textToShow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                // Mostra detalhes do livro indicado
                Text(
                    "Livro: ${indicatedBook.title.ifEmpty { "N/D" }} por ${indicatedBook.author.ifEmpty { "N/D" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Botão para sortear novamente (apenas para admin e se o ciclo acabou)
                if (isAdmin && daysRemaining < 1) {
                    Button(onClick = onSortearClick) {
                        Text("Sortear novo usuário")
                    }
                }
            }
            }
        }
    }



@Composable
fun ClubHeader(
    club: BookClub?,
    onBackClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        AsyncImage(
            model = club?.imageUrl ?: "",
            contentDescription = "Capa do Clube",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_launcher_background), // Usar placeholder melhor depois
            error = painterResource(id = R.drawable.ic_launcher_background)
        )
        Box( // Gradiente para escurecer a parte inferior
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 50f // Onde o gradiente começa
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars) // Empurra o conteúdo abaixo da status bar
        )
        // Botão Voltar
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
        }

        // Botão Admin (Configurações) - TODO: Verificar se é admin antes de mostrar?
        IconButton(
            onClick = onAdminClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Configurações do Clube", tint = Color.White)
        }

        // Nome do Clube
        Text(
            text = club?.name ?: "Carregando...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

@Composable
fun MessageList(
    messages: List<Message>,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier // Recebe o modifier
) {
    val listState = rememberLazyListState()
    val chatItems = remember(messages) { messages.withDayHeaders() }

    // Rola para a última mensagem quando a lista muda
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Use coroutine para garantir que a rolagem aconteça após a composição
            listState.animateScrollToItem(chatItems.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier, // Aplica o modifier recebido (geralmente .weight(1f))
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Espaço entre mensagens
    ) {
        items(chatItems) { item ->
            when (item) {
                is ChatListItem.DayHeader -> DaySeparator(item.label)
                is ChatListItem.ChatMessage -> MessageItem(message = item.message, onProfileClick = onProfileClick)
            }
        }
    }
}

@Composable
fun DaySeparator(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(text = label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp)
        }
    }
}

@Composable
fun MessageItem(
    message: Message, // Recebe a mensagem a ser exibida
    onProfileClick: (String) -> Unit // Recebe a ação de clique no perfil
) {
    val currentUserId = Firebase.auth.currentUser?.uid
    val isCurrentUser = message.senderId == currentUserId

    Row(
        modifier = Modifier.fillMaxWidth(),
        // Alinha a mensagem à direita se for do usuário atual, senão à esquerda
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Row(verticalAlignment = Alignment.Top) { // Alinha foto e coluna de texto
            // Mostra a foto apenas se NÃO for o usuário atual
            if (!isCurrentUser) {
                AsyncImage(
                    model = message.senderPhotoUrl.ifEmpty { R.drawable.ic_launcher_background }, // Placeholder
                    contentDescription = "Foto de ${message.senderName}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(message.senderId) }, // Permite clicar na foto
                    contentScale = ContentScale.Crop,
                    // Adiciona placeholders/errors melhores se necessário
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    error = painterResource(id = R.drawable.ic_launcher_background)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Coluna com nome (se não for user atual), balão da mensagem e hora
            Column(horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start) {
                // Mostra o nome do remetente apenas se NÃO for o usuário atual
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Balão da mensagem
                Surface(
                    shape = RoundedCornerShape( // Cantos arredondados específicos
                        topStart = 8.dp, topEnd = 8.dp,
                        bottomStart = if (isCurrentUser) 8.dp else 0.dp, // Canto inferior esquerdo reto se não for user atual
                        bottomEnd = if (isCurrentUser) 0.dp else 8.dp // Canto inferior direito reto se for user atual
                    ),
                    // Cores diferentes para diferenciar
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    // Texto da mensagem dentro do balão
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                // Hora da mensagem
                Text(
                    text = message.timestamp.toFormattedString(), // Usa a função de formatação
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun MessageInput(
    text: String, // Texto atual no campo
    onTextChange: (String) -> Unit, // Função para atualizar o texto
    onSendClick: () -> Unit, // Função para enviar a mensagem
    isQuickMenuExpanded: Boolean,
    onToggleQuickMenu: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit
) {
    Surface(shadowElevation = 8.dp) { // Sombra para destacar
        Column {
            if (isQuickMenuExpanded) {
                QuickActionMenu(onQuickActionClick = onQuickActionClick)
            }
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars) // Afasta do fundo (gestos/botões)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de Menu
            FilledTonalIconButton(onClick = onToggleQuickMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Opções")
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Campo de Texto
            OutlinedTextField(
                value = text, // Mostra o texto atual
                onValueChange = onTextChange, // Atualiza o texto quando digitado
                modifier = Modifier.weight(1f), // Ocupa espaço
                placeholder = { Text("Digite uma mensagem") },
                shape = RoundedCornerShape(24.dp), // Bordas arredondadas
                singleLine = true, // Evita quebra de linha
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), // Botão "Enviar" no teclado
                keyboardActions = KeyboardActions(onSend = { onSendClick() }), // Ação ao clicar no botão do teclado
                trailingIcon = { // Ícone dentro do campo
                    IconButton(onClick = onSendClick) { // Botão de enviar
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            )
            }
        }
    }
}



enum class QuickAction(val label: String) {
    Recommendation("Pedir recomendação"),
    History("Histórico"),
    Gamification("Gamificação")
}

@Composable
private fun QuickActionMenu(onQuickActionClick: (QuickAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(Icons.Default.AutoAwesome, "Recomendação") {
            onQuickActionClick(QuickAction.Recommendation)
        }
        QuickActionButton(Icons.Default.History, "Histórico") {
            onQuickActionClick(QuickAction.History)
        }
        QuickActionButton(Icons.Default.SportsEsports, "Gamificação") {
            onQuickActionClick(QuickAction.Gamification)
        }
    }
}

@Composable
private fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(icon, contentDescription = text)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun QuickActionDialog(
    action: QuickAction,
    club: BookClub?,
    recommendations: List<ValidatedRecommendation>,
    isLoadingRecommendations: Boolean,
    recommendationInput: String,
    onRecommendationInputChange: (String) -> Unit,
    onRequestRecommendations: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (action) {
        QuickAction.Recommendation -> "Pedir recomendação" to ""
        QuickAction.History -> "Histórico do clube" to formatHistory(club)
        QuickAction.Gamification -> "Gamificação" to "Conquistas como Leitor da Madrugada e Fominha aparecem aqui conforme participação no chat e leitura."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (action == QuickAction.Recommendation) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = recommendationInput,
                        onValueChange = onRecommendationInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex.: queremos fantasia curta e contemporânea") },
                        maxLines = 3,
                        singleLine = false
                    )

                    Button(onClick = onRequestRecommendations) {
                        Text("Gerar recomendações")
                    }

                    when {
                        isLoadingRecommendations -> CircularProgressIndicator()
                        recommendations.isEmpty() -> Text("Nenhuma recomendação ainda. Informe o que o grupo quer ler e toque em Gerar recomendações.")
                        else -> {
                            recommendations.forEach { item ->
                                val title = item.bookItem.volumeInfo?.title ?: item.recommendation.title
                                val author = item.bookItem.volumeInfo?.authors?.joinToString() ?: item.recommendation.author
                                Text("• $title - $author\n${item.recommendation.reason}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            } else {
                Text(message)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Fechar") } }
    )
}

private fun formatHistory(club: BookClub?): String {
    val history = club?.readingHistory.orEmpty()
    if (history.isEmpty()) return "Ainda não há livros registrados no histórico."
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    return history
        .sortedByDescending { it.startDate }
        .joinToString("\n\n") { entry ->
            val start = if (entry.startDate > 0L) formatter.format(Date(entry.startDate)) else "--/--/----"
            val end = if (entry.endDate > 0L) formatter.format(Date(entry.endDate)) else "--/--/----"
            "• ${entry.title} - ${entry.author}\n  $start - $end"
        }
}

private sealed interface ChatListItem {
    data class DayHeader(val label: String) : ChatListItem
    data class ChatMessage(val message: Message) : ChatListItem
}

private fun List<Message>.withDayHeaders(now: Date = Date()): List<ChatListItem> {
    if (isEmpty()) return emptyList()
    val sorted = this.sortedBy { it.timestamp?.time ?: 0L }
    val items = mutableListOf<ChatListItem>()
    var currentKey: String? = null

    for (message in sorted) {
        val ts = message.timestamp ?: continue
        val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(ts)
        if (key != currentKey) {
            items += ChatListItem.DayHeader(ts.toDayHeaderLabel(now))
            currentKey = key
        }
        items += ChatListItem.ChatMessage(message)
    }
    return items
}

private fun Date.toDayHeaderLabel(now: Date = Date()): String {
    val today = Calendar.getInstance().apply { time = now }
    val target = Calendar.getInstance().apply { time = this@toDayHeaderLabel }

    val sameDay = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Hoje"

    today.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Ontem"

    return if (Calendar.getInstance().get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
        SimpleDateFormat("EEEE", Locale.forLanguageTag("pt-BR")).format(this).replaceFirstChar { it.uppercase() }
    } else {
        SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")).format(this)
    }
}

// Função utilitária para formatar a data/hora da mensagem
private fun Date?.toFormattedString(): String {
    if (this == null) return ""
    return try {
        // Formato simples HH:mm
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(this)
    } catch (e: Exception) {
        Log.e("ClubScreenFormatting", "Erro ao formatar data: $this", e)
        "" // Retorna vazio em caso de erro
    }
}

// --- PREVIEW ---
@Preview(showBackground = true)
@Composable
fun ClubScreenContentPreview() {
    // Dados de exemplo para o preview
    val sampleMessages = listOf(
        Message(senderName = "Yago", text = "Olá pessoal! Vamos começar a ler?", timestamp = Date()),
        Message(senderName = "Fulano", text = "Claro! Já estou na página 50.", timestamp = Date())
    )
    val clubState3 = sampleClubsList.first().copy(
        currentUserForCycleId = "123",
        indicatedBook = IndicatedBook(title = "O Pequeno Príncipe", author = "Antoine de Saint-Exupéry"),
        cycleEndDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(6)
    )

    CapitularIATheme {
        ClubScreenContent(
            club = clubState3,
            messages = sampleMessages,
            sortedUser = sampleUser,
            inputText = "Minha nova mensagem",
            onInputChange = {},
            onSendMessage = {},
            onSortearClick = {},
            onSearchBookClick = {}, // Adicionado para o preview compilar
            isLoading = false,
            accessDenied = false,
            onBackClick = {},
            onProfileClick = {},
            onAdminClick = {},
            isQuickMenuExpanded = false,
            onToggleQuickMenu = {},
            onQuickActionClick = {}
        )
    }
}


@Composable
private fun ClubPreferencesSection(club: BookClub) {
    if (club.preferredGenres.isEmpty() && club.preferredTags.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (club.preferredGenres.isNotEmpty()) {
            Text(
                text = "Gêneros: ${club.preferredGenres.joinToString(" • ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (club.preferredTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tags: ${club.preferredTags.joinToString(" • ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
