package br.com.CapitularIA.ui.features.club

// --- Imports Essenciais ---
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.CapitularIA.R
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.IndicatedBook
import br.com.CapitularIA.data.Message
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.sampleClubsList
import br.com.CapitularIA.data.sampleUser
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.CapitularIATheme
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
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

    var inputText by remember { mutableStateOf("") } // Estado local para o campo de mensagem
    val context = LocalContext.current

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
        onMenuClick = {
            // Ação temporária para o botão de menu
            Toast.makeText(context, "Menu de opções", Toast.LENGTH_SHORT).show()
        }
    )
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
    onMenuClick: () -> Unit
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
                        onMenuClick = onMenuClick // Passa a função do menu
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
                    Text("Aguardando ${sortedUserName} indicar um livro.", textAlign = TextAlign.Center)
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
                    "Livro: ${indicatedBook.title ?: "N/D"} por ${indicatedBook.author ?: "N/D"}",
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
            color = Color.White,
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

    // Rola para a última mensagem quando a lista muda
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Use coroutine para garantir que a rolagem aconteça após a composição
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier, // Aplica o modifier recebido (geralmente .weight(1f))
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Espaço entre mensagens
    ) {
        items(messages) { message -> // Itera sobre a lista de mensagens
            MessageItem(message = message, onProfileClick = onProfileClick) // Passa cada mensagem
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
    onMenuClick: () -> Unit // Função para o botão de menu
) {
    Surface(shadowElevation = 8.dp) { // Sombra para destacar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars) // Afasta do fundo (gestos/botões)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de Menu
            FilledTonalIconButton(onClick = onMenuClick) {
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
            onMenuClick = {}
        )
    }
}