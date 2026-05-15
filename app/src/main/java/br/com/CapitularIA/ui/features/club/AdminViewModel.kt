package br.com.CapitularIA.ui.features.club

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

// "Molde" para a UI de Solicitações
data class JoinRequest(
    val userId: String,
    val user: User
)

// Resultado da ação de sair
sealed class LeaveClubResult {
    data object IDLE : LeaveClubResult()
    data object SUCCESS : LeaveClubResult()
    data class ERROR(val message: String) : LeaveClubResult()
}

// Resultado da ação de excluir
sealed class DeleteClubResult {
    data object IDLE : DeleteClubResult()
    data object SUCCESS : DeleteClubResult()
}

class AdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    val currentUserId = auth.currentUser?.uid

    private val _club = MutableLiveData<BookClub?>()
    val club: LiveData<BookClub?> = _club

    private val _requests = MutableLiveData<List<JoinRequest>>()
    val requests: LiveData<List<JoinRequest>> = _requests

    private val _members = MutableLiveData<List<User>>()
    val members: LiveData<List<User>> = _members

    private val _isLoadingRequests = MutableLiveData(false)
    val isLoadingRequests: LiveData<Boolean> = _isLoadingRequests

    private val _isLoadingMembers = MutableLiveData(false)
    val isLoadingMembers: LiveData<Boolean> = _isLoadingMembers

    private val _toastMessage = MutableLiveData<String?>(null)
    val toastMessage: LiveData<String?> = _toastMessage

    private val _leaveResult = MutableLiveData<LeaveClubResult>(LeaveClubResult.IDLE)
    val leaveResult: LiveData<LeaveClubResult> = _leaveResult

    private val _clubDeleted = MutableLiveData<DeleteClubResult>(DeleteClubResult.IDLE)
    val clubDeleted: LiveData<DeleteClubResult> = _clubDeleted

    private lateinit var currentClubId: String

    fun loadAdminData(clubId: String) {
        currentClubId = clubId
        _isLoadingRequests.value = true
        _isLoadingMembers.value = true

        db.collection("clubs").document(clubId)
            .addSnapshotListener { clubSnapshot, error ->
                if (error != null || clubSnapshot == null) {
                    Log.e("AdminViewModel", "Erro ao carregar clube", error)
                    _isLoadingRequests.value = false
                    _isLoadingMembers.value = false
                    return@addSnapshotListener
                }

                val clubData = clubSnapshot.toObject(BookClub::class.java)
                _club.value = clubData

                if (clubData == null) {
                    _isLoadingRequests.value = false
                    _isLoadingMembers.value = false
                    return@addSnapshotListener
                }

                val requestIds = clubData.joinRequests
                if (requestIds.isEmpty()) {
                    _requests.value = emptyList()
                    _isLoadingRequests.value = false
                } else {
                    fetchUsersFromIds(requestIds, isRequest = true)
                }

                val memberIds = clubData.members
                if (memberIds.isEmpty()) {
                    _members.value = emptyList()
                    _isLoadingMembers.value = false
                } else {
                    fetchUsersFromIds(memberIds, isRequest = false)
                }
            }
    }

    private fun fetchUsersFromIds(userIds: List<String>, isRequest: Boolean) {
        if (userIds.isEmpty()) {
            if (isRequest) _isLoadingRequests.value = false else _isLoadingMembers.value = false
            return
        }

        viewModelScope.launch {
            try {
                val usersSnapshot = db.collection("users").whereIn("uid", userIds).get().await()
                val users = usersSnapshot.toObjects(User::class.java)

                if (isRequest) {
                    val joinRequests = userIds.mapNotNull { id ->
                        users.find { it.uid == id }?.let { user ->
                            JoinRequest(userId = id, user = user)
                        }
                    }
                    _requests.value = joinRequests
                } else {
                    _members.value = users
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao buscar perfis de usuários", e)
                if (isRequest) _requests.value = emptyList() else _members.value = emptyList()
            } finally {
                if (isRequest) _isLoadingRequests.value = false else _isLoadingMembers.value = false
            }
        }
    }

    fun approveRequest(userId: String) {
        viewModelScope.launch {
            try {
                val clubRef = db.collection("clubs").document(currentClubId)
                db.runTransaction { transaction ->
                    val club = transaction.get(clubRef).toObject(BookClub::class.java)
                    if (club != null && club.members.size >= club.maxMembers) {
                        throw Exception("O clube está lotado.")
                    }

                    transaction.update(clubRef, "joinRequests", FieldValue.arrayRemove(userId))
                    transaction.update(clubRef, "members", FieldValue.arrayUnion(userId))
                    null
                }.await()
                _toastMessage.value = "Usuário aprovado!"
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao aprovar usuário", e)
                _toastMessage.value = e.message ?: "Erro ao aprovar."
            }
        }
    }

    fun denyRequest(userId: String) {
        viewModelScope.launch {
            try {
                db.collection("clubs").document(currentClubId)
                    .update("joinRequests", FieldValue.arrayRemove(userId))
                    .await()
                _toastMessage.value = "Usuário negado."
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao negar usuário", e)
                _toastMessage.value = "Erro ao negar."
            }
        }
    }

    fun kickMember(userId: String) {
        viewModelScope.launch {
            try {
                db.collection("clubs").document(currentClubId)
                    .update("members", FieldValue.arrayRemove(userId))
                    .await()
                _toastMessage.value = "Membro removido."
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao expulsar membro", e)
                _toastMessage.value = "Erro ao remover membro."
            }
        }
    }

    fun leaveClub() {
        viewModelScope.launch {
            if (currentUserId == null) return@launch

            val currentClub = _club.value
            if (currentClub == null) {
                _toastMessage.value = "Erro: Clube não encontrado."
                return@launch
            }

            if (currentClub.adminId == currentUserId) {
                _toastMessage.value = "Admins não podem sair. Transfira a administração primeiro."
                return@launch
            }

            try {
                db.collection("clubs").document(currentClubId)
                    .update("members", FieldValue.arrayRemove(currentUserId))
                    .await()
                _toastMessage.value = "Você saiu do clube."
                _leaveResult.value = LeaveClubResult.SUCCESS
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao sair do clube", e)
                _leaveResult.value = LeaveClubResult.ERROR("Erro ao sair do clube.")
            }
        }
    }

    fun drawUserForCycle() {
        viewModelScope.launch {
            val currentClub = _club.value
            val adminId = auth.currentUser?.uid

            if (currentClub == null || adminId == null || currentClub.adminId != adminId) {
                _toastMessage.value = "Apenas o admin pode sortear."
                return@launch
            }
            if (currentClub.members.isEmpty()) {
                _toastMessage.value = "Não há membros no clube para sortear."
                return@launch
            }

            val randomUserId = currentClub.members.random()
            val updates = mapOf(
                "currentUserForCycleId" to randomUserId,
                "indicatedBook" to null,
                "cycleEndDate" to null
            )

            try {
                db.collection("clubs").document(currentClub.id).update(updates).await()
                _toastMessage.value = "Novo usuário sorteado!"
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao sortear usuário", e)
                _toastMessage.value = "Erro ao tentar sortear usuário."
            }
        }
    }

    // ✅ Nova função: verifica se o clube pode ser deletado
    fun canDeleteClub(): Boolean {
        val currentClub = _club.value
        val userId = currentUserId

        if (currentClub == null || userId == null) {
            _toastMessage.value = "Erro: Clube não encontrado."
            return false
        }

        if (currentClub.adminId != userId) {
            _toastMessage.value = "Apenas o admin pode excluir o clube."
            return false
        }

        if (currentClub.members.size > 1) {
            _toastMessage.value = "Você precisa ser o único membro para excluir o clube."
            return false
        }

        return true
    }

    // Função limpa apenas para deletar
    fun deleteClub() {
        viewModelScope.launch {
            try {
                db.collection("clubs").document(currentClubId).delete().await()
                _toastMessage.value = "Clube excluído com sucesso."
                _clubDeleted.value = DeleteClubResult.SUCCESS
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Erro ao excluir clube", e)
                _toastMessage.value = "Erro ao excluir o clube."
            }
        }
    }

    fun onToastMessageShown() {
        _toastMessage.value = null
    }

    fun resetLeaveResult() {
        _leaveResult.value = LeaveClubResult.IDLE
    }
}
