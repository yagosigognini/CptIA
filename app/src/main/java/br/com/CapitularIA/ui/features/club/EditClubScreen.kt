package br.com.CapitularIA.ui.features.club

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.R
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.features.createclub.ModeHelpDialog
import br.com.CapitularIA.ui.features.createclub.ModeSelector
import br.com.CapitularIA.ui.theme.ButtonRed
import br.com.CapitularIA.ui.theme.TextColor
import coil.compose.AsyncImage

// ✅ ANOTAÇÃO ADICIONADA AQUI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClubScreen(
    clubId: String,
    viewModel: EditClubViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    LaunchedEffect(clubId) {
        viewModel.loadClub(clubId)
    }

    val club by viewModel.club.observeAsState()
    val updateStatus by viewModel.updateStatus.observeAsState()
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var clubName by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var maxMembers by remember { mutableStateOf(10) }
    var description by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val memberOptions = (2..20).toList()
    val maxDescriptionChars = 140

    LaunchedEffect(club) {
        club?.let {
            clubName = it.name
            isPublic = it.isPublic
            maxMembers = it.maxMembers
            description = it.description
        }
    }

    LaunchedEffect(updateStatus) {
        if (updateStatus == UpdateStatus.SUCCESS) {
            Toast.makeText(context, "Clube atualizado!", Toast.LENGTH_SHORT).show()
            viewModel.resetUpdateStatus()
            onSaveSuccess()
        } else if (updateStatus == UpdateStatus.ERROR) {
            Toast.makeText(context, "Falha ao atualizar.", Toast.LENGTH_SHORT).show()
            viewModel.resetUpdateStatus()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    val isLoading = updateStatus == UpdateStatus.LOADING || club == null

    AppBackground(backgroundResId = R.drawable.background) {
        Scaffold(
            topBar = {
                EditClubTopBar(onBackClick = onBackClick, isLoading = isLoading)
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (club == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        AsyncImage(
                            model = imageUri ?: club?.imageUrl ?: R.drawable.ic_launcher_background,
                            contentDescription = "Capa do Clube",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        TextButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            enabled = !isLoading
                        ) {
                            Text("Alterar Capa", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.padding(24.dp)) {
                        OutlinedTextField(
                            value = clubName,
                            onValueChange = { clubName = it },
                            label = { Text("Nome") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Column {
                            Text("Modo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ModeSelector(
                                    isPublic = isPublic,
                                    onModeSelected = { isPublic = it },
                                    enabled = !isLoading
                                )
                                IconButton(onClick = { showHelpDialog = true }) {
                                    Icon(Icons.Default.Info, contentDescription = "Ajuda sobre os modos", tint = TextColor)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Número máximo de membros", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ExposedDropdownMenuBox(
                                expanded = isDropdownExpanded,
                                onExpandedChange = { if (!isLoading) isDropdownExpanded = it },
                                modifier = Modifier.width(100.dp)
                            ) {
                                OutlinedTextField(
                                    value = maxMembers.toString(),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    enabled = !isLoading
                                )
                                ExposedDropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false }
                                ) {
                                    memberOptions.forEach { number ->
                                        DropdownMenuItem(
                                            text = { Text(number.toString()) },
                                            onClick = {
                                                maxMembers = number
                                                isDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= maxDescriptionChars) description = it },
                            label = { Text("Descrição") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            supportingText = {
                                Text(text = "${description.length} / $maxDescriptionChars", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                            },
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                viewModel.updateClub(clubName, description, isPublic, maxMembers, imageUri)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonRed),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("SALVAR ALTERAÇÕES", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        ModeHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

// ✅ ANOTAÇÃO ADICIONADA AQUI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditClubTopBar(onBackClick: () -> Unit, isLoading: Boolean) {
    TopAppBar(
        title = { Text("Editar Clube") },
        navigationIcon = {
            IconButton(onClick = onBackClick, enabled = !isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        windowInsets = WindowInsets.statusBars
    )
}