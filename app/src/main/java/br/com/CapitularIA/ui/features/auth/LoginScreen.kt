package br.com.CapitularIA.ui.features.auth

import android.widget.Toast
// ✅ --- IMPORTS ADICIONADOS ---
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
// ---
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.CapitularIA.ui.components.AppBackground
import br.com.CapitularIA.ui.theme.CapitularIATheme
import br.com.CapitularIA.ui.theme.ButtonRed
import br.com.CapitularIA.ui.theme.TextColor
import br.com.CapitularIA.R

// ✅ --- LÓGICA DE LOGIN COM GOOGLE ADICIONADA AQUI ---
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authResult by viewModel.authResult.observeAsState(AuthResult.IDLE)
    val context = LocalContext.current

    // 1. O Launcher que espera o resultado da tela de login do Google
    val googleAuthLauncher = rememberLauncherForActivityResult( // Renomeado para clareza
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // 2. Sucesso: Pega a conta e o token
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                // 3. Envia a credencial para o ViewModel
                viewModel.signInWithGoogle(credential, onLoginSuccess)
            } catch (e: ApiException) {
                // 4. Falha: Mostra erro
                Log.w("LoginScreen", "Google sign in failed", e)
                Toast.makeText(context, "Falha no login com Google.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // 5. O Cliente do Google SignIn
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // O R.string.default_web_client_id é gerado automaticamente
            // pelo seu arquivo google-services.json. Não precisa criar!
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.SUCCESS -> {
                viewModel.resetAuthResult()
                // A navegação é chamada pelo onLoginSuccess()
            }
            is AuthResult.ERROR -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.resetAuthResult()
            }
            else -> {}
        }
    }

    LoginScreenContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        onLoginClick = { viewModel.login(email, password, onLoginSuccess) },
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = { viewModel.sendPasswordReset(email) },

        // ⬇️ --- AÇÃO DE CLIQUE CORRIGIDA --- ⬇️
        onGoogleLoginClick = {
            // 1. FAZ SIGN OUT DO CLIENTE GOOGLE (LIMPA O CACHE)
            googleSignInClient.signOut().addOnCompleteListener {
                // 2. DEPOIS DE LIMPAR, LANÇA A INTENÇÃO DE LOGIN
                // Isso força a seleção de conta a aparecer.
                val signInIntent = googleSignInClient.signInIntent
                googleAuthLauncher.launch(signInIntent)
            }
        },
        // ⬆️ --- FIM DA CORREÇÃO --- ⬆️

        isLoading = authResult is AuthResult.LOADING
    )
}

@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    isLoading: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AppBackground(backgroundResId = R.drawable.app_background) {
        // ✅ MUDANÇA: Sai Box, entra Column rolável
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars) // Protege das barras de sistema
                .verticalScroll(rememberScrollState()) // Adiciona rolagem
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Distribui o espaço em telas grandes, permite rolar em pequenas
            verticalArrangement = Arrangement.SpaceAround
        ) {

            // Spacer flexível (opcional, mas ajuda a centralizar)
            // Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.logo_completa),
                contentDescription = "Logo Letraria",
                // ✅ MUDANÇA: Tamanho fixo trocado por relativo
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Ocupa 80% da largura
                    .padding(vertical = 32.dp),
                contentScale = ContentScale.Fit
            )


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(image, null) }
                    },
                    enabled = !isLoading
                )

                TextButton(
                    onClick = onForgotPasswordClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text(
                        "Esqueci minha senha",
                        color = TextColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLoginClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonRed)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("ENTRAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onRegisterClick, enabled = !isLoading) {
                    Text("Não tem uma conta? Cadastre-se", color = TextColor)
                }

                // --- SEPARADOR E BOTÃO DO GOOGLE ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text("OU", modifier = Modifier.padding(horizontal = 8.dp), color = TextColor)
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = onGoogleLoginClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Logo do Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Entrar com Google", color = TextColor, fontWeight = FontWeight.Bold)
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CapitularIATheme {
        AppBackground(backgroundResId = R.drawable.app_background) {
            LoginScreenContent(
                email = "", onEmailChange = {},
                password = "", onPasswordChange = {},
                onLoginClick = {}, onRegisterClick = {},
                onForgotPasswordClick = {},
                onGoogleLoginClick = {},
                isLoading = false
            )
        }
    }
}