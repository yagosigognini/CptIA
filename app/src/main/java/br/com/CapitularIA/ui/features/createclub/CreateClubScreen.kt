package br.com.CapitularIA.ui.features.createclub

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.R
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.ButtonRed
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.ui.theme.TextColor
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClubScreen(
    viewModel: CreateClubViewModel = viewModel(),
    onClose: () -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    var clubName by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) } // Padrão Privado
    var maxMembers by remember { mutableStateOf(10) }
    var description by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }

    // ✅ OBSERVANDO O VIEWMODEL
    val createClubResult by viewModel.createClubResult.observeAsState()
    val isLoading = createClubResult is CreateClubResult.LOADING

    var isDropdownExpanded by remember { mutableStateOf(false) }
    val memberOptions = (2..20).toList()
    val maxDescriptionChars = 140

    AppBackground(backgroundResId = R.drawable.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = imageUri ?: R.drawable.ic_launcher_background,
                    contentDescription = "Capa do Clube",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    error = painterResource(id = R.drawable.ic_launcher_background)
                )
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), enabled = !isLoading) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
                TextButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    enabled = !isLoading
                ) {
                    Text("Alterar Capa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = clubName, onValueChange = { clubName = it },
                    label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(24.dp))
                Column {
                    Text("Modo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeSelector(isPublic = isPublic, onModeSelected = { isPublic = it }, enabled = !isLoading)
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
                            value = maxMembers.toString(), onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier.menuAnchor(),
                            enabled = !isLoading
                        )
                        ExposedDropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
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
                    label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth().height(120.dp),
                    supportingText = {
                        Text(text = "${description.length} / $maxDescriptionChars", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    },
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(32.dp))

                // ✅ BOTÃO CONECTADO AO VIEWMODEL
                Button(
                    onClick = {
                        viewModel.createClub(clubName, description, isPublic, maxMembers, imageUri)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonRed),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("CRIAR CLUBE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        ModeHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

// --- Componentes Auxiliares ---
@Composable
fun ModeSelector(isPublic: Boolean, onModeSelected: (Boolean) -> Unit, enabled: Boolean) {
    val cornerRadius = 50.dp
    Row(
        modifier = Modifier
            .width(200.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(
                    color = if (!isPublic) ButtonRed else Color.Transparent,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clickable(enabled = enabled) { onModeSelected(false) },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Privado", color = if (!isPublic) Color.White else TextColor, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(
                    color = if (isPublic) ButtonRed else Color.Transparent,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clickable(enabled = enabled) { onModeSelected(true) },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Público", color = if (isPublic) Color.White else TextColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModeHelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Modo privado:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "usuários não são capazes de ver o clube na lista de procura. É necessário um convite ou inserir o código para participar.",
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Text("Modo público:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("usuários são capazes de ver o clube na lista de procura. Será necessário autorização do administrador para entrar.",
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("OK")
                }
            }
        }
    }
}

@Preview
@Composable
fun CreateClubScreenPreview() {
    CapitularIATheme {
        CreateClubScreen(onClose = {})
    }
}