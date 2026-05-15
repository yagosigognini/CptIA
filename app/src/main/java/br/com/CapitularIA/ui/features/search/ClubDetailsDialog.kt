package br.com.CapitularIA.ui.features.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.data.User
import br.com.CapitularIA.ui.theme.ButtonRed
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R
import coil.compose.AsyncImage

@Composable
fun ClubDetailsDialog(
    clubId: String,
    viewModel: ClubDetailsViewModel = viewModel(),
    onDismiss: () -> Unit,
    onJoinClick: () -> Unit
) {
    // Pede ao ViewModel para carregar os dados quando o diálogo aparece
    LaunchedEffect(clubId) {
        viewModel.loadClubDetails(clubId)
    }

    val club by viewModel.club.observeAsState()
    val moderator by viewModel.moderator.observeAsState()

    Dialog(onDismissRequest = onDismiss) {
        // Mostra um loading enquanto os dados do clube não chegam
        if (club == null) {
            CircularProgressIndicator()
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                // ✅ ADICIONADO O FUNDO
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Box {
                        AsyncImage(
                            model = club!!.imageUrl,
                            contentDescription = "Capa do Clube",
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(club!!.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Descrição", style = MaterialTheme.typography.titleMedium)
                        Text(club!!.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // ✅ SEÇÃO DO MODERADOR ADICIONADA
                        Text("Moderador", style = MaterialTheme.typography.titleMedium)
                        ModeratorInfo(moderator)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Ciclo de leitura", style = MaterialTheme.typography.titleMedium)
                                Text("${club!!.readingCycleDays} dias", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Membros", style = MaterialTheme.typography.titleMedium)
                                Text("${club!!.members.size} / ${club!!.maxMembers}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onJoinClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonRed)
                        ) {
                            Text("PARTICIPAR")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeratorInfo(moderator: User?) {
    if (moderator == null) {
        // Placeholder enquanto o moderador carrega
        Text("Carregando...", style = MaterialTheme.typography.bodyMedium)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            AsyncImage(
                model = moderator.profilePictureUrl.ifEmpty { R.drawable.background },
                contentDescription = "Foto do moderador",
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(moderator.name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Preview
@Composable
fun ClubDetailsDialogPreview() {
    CapitularIATheme {

    }
}