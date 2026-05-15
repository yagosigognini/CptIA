package br.com.CapitularIA.ui.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R

// --- TELA "INTELIGENTE" (STATEFUL) ---
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccount: () -> Unit,
    onTermsClick: () -> Unit
) {
    val userEmail by remember { mutableStateOf(viewModel.userEmail) }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    SettingsScreenContent(
        userEmail = userEmail,
        onBackClick = onBackClick,
        onPasswordResetClick = { viewModel.sendPasswordResetEmail() },
        onFeedbackClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("CapitularIA@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Feedback sobre o CapitularIA")
            }
            try { context.startActivity(intent) } catch (e: Exception) { /* Lida com erro */ }
        },
        onTermsClick = onTermsClick,
        onLogoutClick = onLogoutClick,
        onDeleteAccountClick = { showDeleteDialog = true }
    )
}

// --- TELA "BURRA" (STATELESS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    userEmail: String,
    onBackClick: () -> Unit,
    onPasswordResetClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onTermsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Configurações", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Seção de Conta
                Text("Conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Email", value = userEmail)
                ClickableRow(label = "Senha", value = "Redefinir", onClick = onPasswordResetClick)

                Spacer(modifier = Modifier.height(24.dp))

                // Seção de Preferências
                Text("Preferências", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Tema", value = "Padrão do Sistema")

                Spacer(modifier = Modifier.height(24.dp))

                // Seção "Sobre"
                Text("Sobre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                ClickableRow(label = "Enviar Feedback", onClick = onFeedbackClick)
                ClickableRow(label = "Termos e Políticas", onClick = onTermsClick)

                Spacer(modifier = Modifier.height(32.dp))

                // ✅ --- BOTÕES RESTAURADOS ---
                // Ações Finais
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair da conta", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDeleteAccountClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir conta", color = MaterialTheme.colorScheme.onErrorContainer)
                }
                // ✅ --- FIM DA RESTAURAÇÃO ---
            }
        }
    }
}

// ... (Resto do arquivo: DeleteAccountDialog, InfoRow, ClickableRow, Preview, TermsAndPoliciesScreen - Sem Mudanças)
@Composable
fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir Conta") },
        text = { Text("Você tem certeza? Esta ação é permanente e não pode ser desfeita. Todos os seus dados serão apagados.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

@Composable
private fun ClickableRow(label: String, value: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    CapitularIATheme {
        SettingsScreenContent(
            userEmail = "fulanodetal@gmail.com",
            onBackClick = {}, onPasswordResetClick = {},
            onFeedbackClick = {},
            onTermsClick = {}, onLogoutClick = {},
            onDeleteAccountClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPoliciesScreen(
    onBackClick: () -> Unit
) {
    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Termos e Políticas") },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Termos de Uso",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Bem-vindo ao Letraria! Ao usar nosso aplicativo, você concorda com estes termos. \n\n" +
                            "1. Contas de Usuário: Você é responsável por manter a confidencialidade da sua conta e senha. Você concorda em aceitar a responsabilidade por todas as atividades que ocorram sob sua conta.\n\n" +
                            "2. Conteúdo Gerado pelo Usuário: Você é o único responsável pelo conteúdo (mensagens, indicações de livros, etc.) que publica nos clubes de leitura. Você não deve postar conteúdo ilegal, odioso ou difamatório.\n\n" +
                            "3. Administração do Clube: Os administradores do clube têm o poder de adicionar ou remover membros de seus respectivos clubes. O Letraria não se responsabiliza por disputas de gerenciamento de clube.\n\n" +
                            "4. Rescisão: Podemos rescindir ou suspender seu acesso ao nosso serviço imediatamente, sem aviso prévio ou responsabilidade, por qualquer motivo, incluindo violação destes Termos.\n\n" +
                            "Estes termos são um modelo para um projeto acadêmico.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Política de Privacidade",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Esta política descreve como coletamos e usamos seus dados.\n\n" +
                            "1. Coleta de Dados: Coletamos as informações que você fornece durante o cadastro (nome, e-mail). Se você usar o Login do Google, coletamos o ID, nome e foto do perfil fornecidos pelo Google.\n\n" +
                            "2. Uso de Dados: Usamos seus dados para identificar você no aplicativo, exibir seu perfil para outros membros do clube e permitir a funcionalidade principal do aplicativo. Não vendemos seus dados a terceiros.\n\n" +
                            "3. Exclusão de Conta: Você pode excluir sua conta a qualquer momento através da tela de 'Configurações'. Ao fazer isso, todos os seus dados pessoais e conteúdo gerado (mensagens, etc.) serão permanentemente removidos de nossos servidores.\n\n" +
                            "Esta política é um modelo para um projeto acadêmico.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}