package br.com.CapitularIA.ui.features.home

// ... Todos os seus imports existentes ...
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import br.com.CapitularIA.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
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
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.Quote
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.sampleClubsList
import br.com.CapitularIA.data.sampleQuote
import br.com.CapitularIA.data.sampleUser
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.components.ClubsSection
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.R
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

private data class UpdateInfo(
    val versionName: String,
    val apkUrl: String
)

val homeButtonColor = Color(0xFFE57373)
val homeTextColor = Color(0xFF2A3A6A)

// --- TELA "INTELIGENTE" (STATEFUL) ---
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateClubClick: () -> Unit,
    onJoinClubClick: () -> Unit,
    onClubClick: (BookClub) -> Unit
) {
    val user by viewModel.user.observeAsState()
    val clubs by viewModel.clubs.observeAsState(emptyList())

    // ✅ MUDANÇA 1: Observe a citação do dia vinda do ViewModel
    val quote by viewModel.quoteOfTheDay.observeAsState(sampleQuote)

    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // --- Lógica de Verificação de Atualização (NÃO MEXA AQUI) ---
    LaunchedEffect(Unit) {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            "latest_version_code" to 1L,
            "latest_version_name" to "1.0",
            "latest_apk_url" to ""
        )).await()

        try {
            remoteConfig.fetchAndActivate().await()
            Log.d("AppUpdate", "Remote config fetched and activated.")

            val latestVersionCode = remoteConfig.getLong("latest_version_code")
            val latestVersionName = remoteConfig.getString("latest_version_name")
            val latestApkUrl = remoteConfig.getString("latest_apk_url")

            val currentVersionCode = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("AppUpdate", "Failed to get current version code", e)
                1L
            }

            Log.d("AppUpdate", "Current Version: $currentVersionCode, Latest Version: $latestVersionCode")

            if (latestVersionCode > currentVersionCode && latestApkUrl.isNotBlank()) {
                Log.d("AppUpdate", "Update available: $latestVersionName, URL: $latestApkUrl")
                updateInfo = UpdateInfo(latestVersionName, latestApkUrl)
            } else {
                Log.d("AppUpdate", "No update available or URL missing.")
                updateInfo = null
            }

        } catch (e: Exception) {
            Log.w("AppUpdate", "Error fetching remote config", e)
            updateInfo = null
        }
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(
            versionName = info.versionName,
            onUpdateClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("AppUpdate", "Failed to open URL", e)
                    Toast.makeText(context, "Não foi possível abrir o link de download.", Toast.LENGTH_SHORT).show()
                }
                updateInfo = null
            },
            onDismiss = {
                updateInfo = null
            }
        )
    }
    // --- Fim da Lógica de Atualização ---


    HomeScreenContent(
        user = user,
        quote = quote, // ✅ MUDANÇA 2: Passe a citação do ViewModel aqui
        clubs = clubs,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onCreateClubClick = onCreateClubClick,
        onJoinClubClick = onJoinClubClick,
        onClubClick = onClubClick
    )
}

// --- TELA "BURRA" (STATELESS) ---
// (Nenhuma mudança necessária aqui)
@Composable
fun HomeScreenContent(
    user: User?,
    quote: Quote,
    clubs: List<BookClub>,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateClubClick: () -> Unit,
    onJoinClubClick: () -> Unit,
    onClubClick: (BookClub) -> Unit
) {
    AppBackground(backgroundResId = R.drawable.app_background) {
        // ✅ MUDANÇA: Sai LazyColumn, entra Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars) // Garante que não fica atrás das barras
                .verticalScroll(rememberScrollState()), // Permite rolar em telas pequenas
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTopBar(
                profileImageUrl = user?.profilePictureUrl,
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick
            )
            Spacer(modifier = Modifier.height(32.dp))
            QuoteSection(quote = quote)
            Spacer(modifier = Modifier.height(32.dp))

            if (clubs.isEmpty()) {
                EmptyClubsSection()
            } else {
                ClubsSection(
                    title = "Meus Clubes",
                    clubs = clubs,
                    onClubClick = onClubClick
                )
            }

            // ✅ MUDANÇA: Este Spacer se estica para empurrar os botões para baixo
            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(24.dp)) // Espaço mínimo
            ActionButtons(onCreateClubClick, onJoinClubClick)
            Spacer(modifier = Modifier.height(32.dp)) // Espaço na parte inferior
        }
    }
}

// --- COMPONENTES ---
// (Nenhuma mudança necessária aqui)
@Composable
fun HomeTopBar(
    profileImageUrl: String?,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = profileImageUrl?.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "Foto de Perfil",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onProfileClick),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_launcher_background),
            error = painterResource(id = R.drawable.ic_launcher_background)
        )

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = homeTextColor)
        }
    }
}

@Composable
fun QuoteSection(quote: Quote) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = quote.text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = quote.source,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButtons(onCreateClubClick: () -> Unit, onJoinClubClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onCreateClubClick,
            modifier = Modifier
                .weight(1f)
                .height(65.dp),
            colors = ButtonDefaults.buttonColors(containerColor = homeButtonColor)
        ) {
            Text("CRIAR CLUBE", color = Color.White)
        }
        Button(
            onClick = onJoinClubClick,
            modifier = Modifier
                .weight(1f)
                .height(65.dp),
            colors = ButtonDefaults.buttonColors(containerColor = homeButtonColor)
        ) {
            Text("BUSCAR CLUBES", color = Color.White)
        }
    }
}

@Composable
fun EmptyClubsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Você ainda não está em nenhum clube",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Crie um novo clube ou use a busca para encontrar um clube e participar!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- PREVIEWS ---
// (Nenhuma mudança necessária aqui)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CapitularIATheme {
        HomeScreenContent(
            user = sampleUser,
            quote = sampleQuote,
            clubs = sampleClubsList,
            onProfileClick = {},
            onSettingsClick = {},
            onCreateClubClick = {},
            onJoinClubClick = {}
        ) {}
    }
}

@Preview(showBackground = true, name = "Home Screen Vazia")
@Composable
fun HomeScreenEmptyPreview() {
    CapitularIATheme {
        HomeScreenContent(
            user = sampleUser,
            quote = sampleQuote,
            clubs = emptyList(),
            onProfileClick = {},
            onSettingsClick = {},
            onCreateClubClick = {},
            onJoinClubClick = {},
            onClubClick = {}
        )
    }
}


// --- Diálogo de Atualização (Sem mudanças) ---
@Composable
private fun UpdateAvailableDialog(
    versionName: String,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualização Disponível") },
        text = { Text("Uma nova versão ($versionName) do Letraria está disponível. Deseja atualizar agora?") },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text("Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Agora Não")
            }
        }
    )
}