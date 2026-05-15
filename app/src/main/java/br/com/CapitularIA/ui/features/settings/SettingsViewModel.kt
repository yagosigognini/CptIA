package br.com.CapitularIA.ui.features.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel // ✅ VOLTOU A SER UM VIEWMODEL NORMAL
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ✅ CLASSE VOLTOU AO NORMAL (NÃO É MAIS AndroidViewModel)
class SettingsViewModel : ViewModel() {
    private val auth = Firebase.auth

    // ✅ --- TODA A LÓGICA DE TEMA FOI REMOVIDA DAQUI ---

    private val _toastMessage = MutableLiveData<String?>(null)
    val toastMessage: LiveData<String?> = _toastMessage

    private val _accountDeleted = MutableLiveData<Boolean>(false)
    val accountDeleted: LiveData<Boolean> = _accountDeleted

    val userEmail: String
        get() = auth.currentUser?.email ?: "Carregando..."

    fun sendPasswordResetEmail() {
        val email = auth.currentUser?.email
        if (!email.isNullOrBlank()) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _toastMessage.value = "E-mail de redefinição de senha enviado!"
                        Log.d("SettingsViewModel", "Password reset email sent.")
                    } else {
                        _toastMessage.value = "Falha ao enviar e-mail. Tente novamente."
                        Log.w("SettingsViewModel", "sendPasswordResetEmail:failure", task.exception)
                    }
                }
        } else {
            _toastMessage.value = "Não foi possível encontrar o e-mail do usuário."
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _toastMessage.value = "Nenhum usuário para excluir."
                return@launch
            }

            try {
                user.delete().await()
                Log.d("SettingsViewModel", "Conta do usuário excluída com sucesso.")
                _accountDeleted.value = true
            } catch (e: Exception) {
                Log.w("SettingsViewModel", "Erro ao excluir conta:", e)
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    _toastMessage.value = "Por segurança, faça o login novamente antes de excluir a conta."
                } else {
                    _toastMessage.value = "Erro ao excluir a conta. Tente novamente."
                }
            }
        }
    }

    fun onToastMessageShown() {
        _toastMessage.value = null
    }
}