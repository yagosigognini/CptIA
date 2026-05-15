package br.com.CapitularIA.ui.features.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class JoinClubResult {
    data object IDLE : JoinClubResult()
    data class SUCCESS(val message: String) : JoinClubResult()
    data class ERROR(val message: String) : JoinClubResult()
}

class ClubDetailsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _club = MutableLiveData<BookClub?>()
    val club: LiveData<BookClub?> = _club

    private val _moderator = MutableLiveData<User?>()
    val moderator: LiveData<User?> = _moderator

    private val _joinResult = MutableLiveData<JoinClubResult>(JoinClubResult.IDLE)
    val joinResult: LiveData<JoinClubResult> = _joinResult

    fun loadClubDetails(clubId: String) {
        viewModelScope.launch {
            db.collection("clubs").document(clubId).get()
                .addOnSuccessListener { clubDocument ->
                    if (clubDocument != null && clubDocument.exists()) {
                        val clubData = clubDocument.toObject(BookClub::class.java)
                        _club.value = clubData

                        if (clubData != null && clubData.adminId.isNotBlank()) {
                            fetchModerator(clubData.adminId)
                        }
                    } else {
                        Log.w("ClubDetailsVM", "Nenhum clube encontrado com o ID: $clubId")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ClubDetailsVM", "Erro ao buscar clube", e)
                }
        }
    }

    private fun fetchModerator(adminId: String) {
        db.collection("users").document(adminId).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument != null && userDocument.exists()) {
                    _moderator.value = userDocument.toObject(User::class.java)
                } else {
                    Log.w("ClubDetailsVM", "Nenhum usuário encontrado com o ID: $adminId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClubDetailsVM", "Erro ao buscar moderador", e)
            }
    }

    // Lógica para entrar em clube PÚBLICO (quando o usuário vê o card)
    fun joinClub() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            val currentClub = _club.value

            if (userId == null || currentClub == null) {
                _joinResult.value = JoinClubResult.ERROR("Erro: Usuário ou clube não encontrado.")
                return@launch
            }

            if (currentClub.members.contains(userId)) {
                _joinResult.value = JoinClubResult.ERROR("Você já é membro deste clube.")
                return@launch
            }
            if (currentClub.joinRequests.contains(userId)) {
                _joinResult.value = JoinClubResult.ERROR("Você já enviou uma solicitação para este clube.")
                return@launch
            }

            try {
                db.collection("clubs").document(currentClub.id)
                    .update("joinRequests", FieldValue.arrayUnion(userId))
                    .await()

                _joinResult.value = JoinClubResult.SUCCESS("Solicitação enviada ao admin do clube!")
            } catch (e: Exception) {
                Log.e("ClubDetailsVM", "Erro ao enviar solicitação", e)
                _joinResult.value = JoinClubResult.ERROR("Ocorreu um erro ao enviar sua solicitação.")
            }
        }
    }

    // ✅ --- NOVA FUNÇÃO PARA ENTRAR COM CÓDIGO ---
    fun requestToJoinByCode(code: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _joinResult.value = JoinClubResult.ERROR("Usuário não autenticado.")
                return@launch
            }
            if (code.isBlank()) {
                _joinResult.value = JoinClubResult.ERROR("O código não pode estar vazio.")
                return@launch
            }

            try {
                // 1. Busca no Firestore pelo clube com o código informado
                val querySnapshot = db.collection("clubs")
                    .whereEqualTo("code", code)
                    .limit(1) // O código deve ser único
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    _joinResult.value = JoinClubResult.ERROR("Nenhum clube encontrado com este código.")
                    return@launch
                }

                val clubDocument = querySnapshot.documents.first()
                val club = clubDocument.toObject(BookClub::class.java)

                if (club == null) {
                    _joinResult.value = JoinClubResult.ERROR("Erro ao ler dados do clube.")
                    return@launch
                }

                // 2. Reutiliza as mesmas verificações da função joinClub()
                if (club.members.contains(userId)) {
                    _joinResult.value = JoinClubResult.ERROR("Você já é membro deste clube.")
                    return@launch
                }
                if (club.joinRequests.contains(userId)) {
                    _joinResult.value = JoinClubResult.ERROR("Você já enviou uma solicitação para este clube.")
                    return@launch
                }

                // 3. Adiciona a solicitação ao clube encontrado
                db.collection("clubs").document(club.id)
                    .update("joinRequests", FieldValue.arrayUnion(userId))
                    .await()

                _joinResult.value = JoinClubResult.SUCCESS("Solicitação enviada ao admin do clube!")

            } catch (e: Exception) {
                Log.e("ClubDetailsVM", "Erro ao solicitar por código", e)
                _joinResult.value = JoinClubResult.ERROR("Erro ao processar sua solicitação.")
            }
        }
    }

    fun resetJoinResult() {
        _joinResult.value = JoinClubResult.IDLE
    }
}