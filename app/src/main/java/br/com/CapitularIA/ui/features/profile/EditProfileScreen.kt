package br.com.CapitularIA.ui.features.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
// ✅ --- IMPORTS ADICIONADOS ---
import androidx.compose.ui.graphics.Color
import br.com.CapitularIA.ui.components.AppBackground
// ---
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.sampleUser
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentUser: User,
    updateStatus: UpdateStatus,
    onSaveClick: (name: String, aboutMe: String, newImageUri: Uri?) -> Unit,
    onBackClick: () -> Unit
) {
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf(currentUser.name) }
    var aboutMe by remember { mutableStateOf(currentUser.aboutMe) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> newImageUri = uri }

    val isLoading = updateStatus == UpdateStatus.LOADING

    // ✅ --- 1. ENVOLVENDO COM O APPBACKGROUND ---
    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            // ✅ --- 2. DEIXANDO O SCAFFOLD TRANSPARENTE ---
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Editar Perfil") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = !isLoading,
                            onClick = { onSaveClick(name, aboutMe, newImageUri) }
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    },
                    // ✅ --- 3. ADICIONANDO OS INSETS (QUE JÁ FIZEMOS ANTES) ---
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = newImageUri ?: currentUser.profilePictureUrl.ifEmpty { R.drawable.ic_launcher_background },
                        contentDescription = "Foto de Perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Alterar Foto")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = aboutMe,
                        onValueChange = { aboutMe = it },
                        label = { Text("Sobre Mim") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    CapitularIATheme {
        EditProfileScreen(
            currentUser = sampleUser,
            updateStatus = UpdateStatus.IDLE,
            onSaveClick = { _, _, _ -> },
            onBackClick = {}
        )
    }
}