package br.com.CapitularIA.ui.features.club

import androidx.compose.foundation.clickable // ✅ ADICIONADO
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.R
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.sampleClubsList
import br.com.CapitularIA.data.sampleUser
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.CapitularIATheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import coil.compose.AsyncImage

@Composable
fun AdminScreen(
    clubId: String,
    viewModel: AdminViewModel = viewModel(),
    onBackClick: () -> Unit,
    onLeaveClub: () -> Unit,
    onDrawUser: () -> Unit,
    onEditClub: () -> Unit,
    onProfileClick: (String) -> Unit // ✅ ADICIONADO: Callback para navegar ao perfil
) {
    LaunchedEffect(clubId) {
        viewModel.loadAdminData(clubId)
    }

    val club by viewModel.club.observeAsState()
    val requests by viewModel.requests.observeAsState(emptyList())
    val members by viewModel.members.observeAsState(emptyList())
    val isLoadingRequests by viewModel.isLoadingRequests.observeAsState(false)
    val isLoadingMembers by viewModel.isLoadingMembers.observeAsState(false)

    val isAdmin = club?.adminId == viewModel.currentUserId

    AdminScreenContent(
        club = club,
        requests = requests,
        members = members,
        isLoadingRequests = isLoadingRequests,
        isLoadingMembers = isLoadingMembers,
        isAdmin = isAdmin,
        onBackClick = onBackClick,
        onApprove = { request -> viewModel.approveRequest(request.userId) },
        onDeny = { request -> viewModel.denyRequest(request.userId) },
        onKick = { member -> viewModel.kickMember(member.uid) },
        onLeaveClub = onLeaveClub,
        onDrawUser = onDrawUser,
        onEditClub = onEditClub,
        onDeleteClub = { viewModel.deleteClub() },
        onProfileClick = onProfileClick // ✅ ATUALIZADO: Passando o callback
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreenContent(
    club: BookClub?,
    requests: List<JoinRequest>,
    members: List<User>,
    isLoadingRequests: Boolean,
    isLoadingMembers: Boolean,
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onApprove: (JoinRequest) -> Unit,
    onDeny: (JoinRequest) -> Unit,
    onKick: (User) -> Unit,
    onLeaveClub: () -> Unit,
    onDrawUser: () -> Unit,
    onEditClub: () -> Unit,
    onDeleteClub: () -> Unit,
    onProfileClick: (String) -> Unit // ✅ ADICIONADO: Recebendo o callback
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = remember(isAdmin) {
        if (isAdmin) listOf("Solicitações", "Membros", "Configurações")
        else listOf("Membros", "Configurações")
    }

    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isAdmin) "Painel do Admin" else "Configurações do Clube") },
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
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                if (isAdmin) {
                    when (selectedTabIndex) {
                        0 -> RequestsTab(requests, isLoadingRequests, onApprove, onDeny, onProfileClick) // ✅ ATUALIZADO
                        1 -> MembersTab(members, isLoadingMembers, club?.adminId, isAdmin, onKick, onProfileClick) // ✅ ATUALIZADO
                        2 -> ConfigTab(club, true, viewModel(), onLeaveClub, onDrawUser, onEditClub)
                    }
                } else {
                    when (selectedTabIndex) {
                        0 -> MembersTab(members, isLoadingMembers, club?.adminId, isAdmin, onKick, onProfileClick) // ✅ ATUALIZADO
                        1 -> ConfigTab(club, false, viewModel(), onLeaveClub, onDrawUser, onEditClub)
                    }
                }
            }
        }
    }
}

// --- ABAS ---
@Composable
fun RequestsTab(
    requests: List<JoinRequest>,
    isLoading: Boolean,
    onApprove: (JoinRequest) -> Unit,
    onDeny: (JoinRequest) -> Unit,
    onProfileClick: (String) -> Unit // ✅ ADICIONADO
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma solicitação pendente.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                RequestItem(
                    user = request.user,
                    onApprove = { onApprove(request) },
                    onDeny = { onDeny(request) },
                    onProfileClick = { onProfileClick(request.user.uid) } // ✅ ATUALIZADO
                )
            }
        }
    }
}

@Composable
fun MembersTab(
    members: List<User>,
    isLoading: Boolean,
    adminId: String?,
    currentUserIsAdmin: Boolean,
    onKick: (User) -> Unit,
    onProfileClick: (String) -> Unit // ✅ ADICIONADO
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(members) { user ->
                MemberItem(
                    user = user,
                    isThisMemberAdmin = user.uid == adminId,
                    currentUserIsAdmin = currentUserIsAdmin,
                    onKick = { onKick(user) },
                    onProfileClick = { onProfileClick(user.uid) } // ✅ ATUALIZADO
                )
            }
        }
    }
}

@Composable
fun ConfigTab(
    club: BookClub?,
    isAdmin: Boolean,
    viewModel: AdminViewModel,
    onLeaveClub: () -> Unit,
    onDrawUser: () -> Unit,
    onEditClub: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (club == null) {
            CircularProgressIndicator()
            return
        }

        Text(
            "Configurações do Clube",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (club.code != null) {
            OutlinedTextField(
                value = club.code,
                onValueChange = {},
                label = { Text("Código de Convite") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isAdmin) {
            Button(onClick = onDrawUser, modifier = Modifier.fillMaxWidth()) {
                Text("Sortear Novo Usuário")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onEditClub, modifier = Modifier.fillMaxWidth()) {
                Text("EDITAR INFORMAÇÕES DO CLUBE")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Botão Deletar Clube com verificação antes do diálogo
            Button(
                onClick = {
                    if (viewModel.canDeleteClub()) {
                        showDeleteDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("DELETAR CLUBE", color = MaterialTheme.colorScheme.onError)
            }

            Text(
                "Só é possível deletar se você for o único membro.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        Button(
            onClick = onLeaveClub,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = if (isAdmin) "Transferir Administração" else "Sair do Clube",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        if (isAdmin) {
            Text(
                "Admins não podem sair, devem transferir a administração.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Dialogo de confirmação de exclusão
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja excluir este clube? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClub()
                        showDeleteDialog = false
                    }
                ) { Text("EXCLUIR") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("CANCELAR") }
            }
        )
    }
}


// --- COMPONENTES DAS ABAS ---
@Composable
fun RequestItem(
    user: User,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onProfileClick: () -> Unit // ✅ ADICIONADO
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfileClick), // ✅ ATUALIZADO: Card clicável
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.profilePictureUrl.ifEmpty { R.drawable.ic_launcher_background },
                contentDescription = "Foto de ${user.name}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(user.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            TextButton(onClick = onDeny) {
                Text("Negar")
            }
            Button(onClick = onApprove) {
                Text("Aprovar")
            }
        }
    }
}

@Composable
fun MemberItem(
    user: User,
    isThisMemberAdmin: Boolean,
    currentUserIsAdmin: Boolean,
    onKick: () -> Unit,
    onProfileClick: () -> Unit // ✅ ADICIONADO
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfileClick), // ✅ ATUALIZADO: Card clicável
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.profilePictureUrl.ifEmpty { R.drawable.ic_launcher_background },
                contentDescription = "Foto de ${user.name}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if(isThisMemberAdmin) {
                    Text("Admin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (currentUserIsAdmin && !isThisMemberAdmin) {
                TextButton(onClick = onKick) {
                    Text("Expulsar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminScreenPreview() {
    // ... (Código do Preview não precisa de alteração) ...
    val requests = listOf(
        JoinRequest("1", sampleUser.copy(name = "Usuário 1")),
        JoinRequest("2", sampleUser.copy(name = "Usuário 2"))
    )
    val members = listOf(
        sampleUser.copy(uid = "admin_id", name = "Admin do Clube"),
        sampleUser.copy(uid = "3", name = "Membro 1")
    )
    val club = sampleClubsList.first().copy(adminId = "admin_id")

    CapitularIATheme {
        AdminScreenContent(
            club = club,
            requests = requests,
            members = members,
            isLoadingRequests = false,
            isLoadingMembers = false,
            isAdmin = true,
            onBackClick = {},
            onApprove = {},
            onDeny = {},
            onKick = {},
            onLeaveClub = {},
            onDrawUser = {},
            onEditClub = {},
            onDeleteClub = {},
            onProfileClick = {} // ✅ ATUALIZADO: Adicionado ao Preview
        )
    }
}