package br.com.CapitularIA.ui.features.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import br.com.CapitularIA.services.updateFcmTokenForCurrentUser

sealed class AuthResult {
    data object IDLE : AuthResult()
    data object LOADING : AuthResult()
    data object SUCCESS : AuthResult()
    data class ERROR(val message: String) : AuthResult()
}

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _authResult = MutableLiveData<AuthResult>(AuthResult.IDLE)
    val authResult: LiveData<AuthResult> = _authResult

    fun login(email: String, password: String, onLoginSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.ERROR("Preencha todos os campos")
            return
        }

        viewModelScope.launch {
            _authResult.value = AuthResult.LOADING
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null && user.isEmailVerified) {
                    updateFcmTokenForCurrentUser()
                    _authResult.value = AuthResult.SUCCESS
                    onLoginSuccess()
                } else {
                    auth.signOut()
                    _authResult.value = AuthResult.ERROR("Por favor, verifique seu e-mail para continuar.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro no login", e)
                _authResult.value = AuthResult.ERROR(e.message ?: "E-mail ou senha incorretos.")
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.ERROR("Preencha todos os campos")
            return
        }
        if (password.length < 6) {
            _authResult.value = AuthResult.ERROR("A senha deve ter pelo menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _authResult.value = AuthResult.LOADING
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                user?.sendEmailVerification()?.await()
                Log.d(TAG, "E-mail de verificação enviado.")

                if (user?.uid != null) {
                    // ✅ A função agora usa o parâmetro padrão (null) para a foto
                    saveUserToFirestore(user.uid, name, email)
                    _authResult.value = AuthResult.SUCCESS
                    Log.d(TAG, "Registro realizado com sucesso")
                } else {
                    _authResult.value = AuthResult.ERROR("Erro ao obter ID do usuário.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro no registro", e)
                _authResult.value = AuthResult.ERROR(e.message ?: "Erro desconhecido ao registrar.")
            }
        }
    }

    // ✅ --- FUNÇÃO ATUALIZADA ---
    // Agora aceita uma photoUrl opcional
    private suspend fun saveUserToFirestore(uid: String, name: String, email: String, photoUrl: String? = null) {
        // Se a photoUrl for nula (ex: cadastro por e-mail), gera um avatar.
        // Se não (ex: login com Google), usa a foto do Google.
        val avatarUrl = photoUrl ?: "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}&background=random"

        val user = hashMapOf(
            "uid" to uid,
            "name" to name,
            "name_lowercase" to name.lowercase(Locale.ROOT), // ⬅️ ADICIONE ESTA LINHA
            "email" to email,
            "profilePictureUrl" to avatarUrl,
            "aboutMe" to "",
            "friendCount" to 0L, // Seu User.kt espera Long, o que é ótimo
            "checklist" to emptyList<Any>(),
            "clubs" to emptyList<Any>(),
            // ✅ Adicione o campo coverPhotoUrl vazio por padrão
            "coverPhotoUrl" to "" // ⬅️ ADICIONE ESTA LINHA
        )
        // Usamos .set(user, SetOptions.merge()) para não sobrescrever dados
        // caso o usuário já exista (o que não deve acontecer com a lógica de new user)
        firestore.collection("users").document(uid).set(user).await()
    }

    // ✅ --- NOVA FUNÇÃO DE LOGIN COM GOOGLE ---
    fun signInWithGoogle(credential: AuthCredential, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _authResult.value = AuthResult.LOADING
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                val isNewUser = result.additionalUserInfo?.isNewUser == true

                // Se for um usuário novo, salvamos no Firestore
                if (isNewUser && user != null) {
                    saveUserToFirestore(
                        uid = user.uid,
                        name = user.displayName ?: "Usuário",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString()
                    )
                }

                // Se for um usuário existente, ele só faz o login
                updateFcmTokenForCurrentUser()
                _authResult.value = AuthResult.SUCCESS
                onLoginSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Erro no login com Google", e)
                _authResult.value = AuthResult.ERROR(e.message ?: "Erro desconhecido no login com Google.")
            }
        }
    }


    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authResult.value = AuthResult.ERROR("Por favor, digite seu e-mail no campo acima.")
            return
        }

        viewModelScope.launch {
            _authResult.value = AuthResult.LOADING
            try {
                auth.sendPasswordResetEmail(email).await()
                _authResult.value = AuthResult.ERROR("E-mail de redefinição enviado! Verifique sua caixa de entrada.")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao redefinir senha", e)
                _authResult.value = AuthResult.ERROR(e.message ?: "Falha ao enviar e-mail.")
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.IDLE
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}