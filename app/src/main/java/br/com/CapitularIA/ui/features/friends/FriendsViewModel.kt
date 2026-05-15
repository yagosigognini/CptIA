package br.com.CapitularIA.ui.features.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.Friend
import br.com.CapitularIA.data.FriendRequest
import br.com.CapitularIA.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val currentUserId = auth.currentUser?.uid

    private var receivedListener: ListenerRegistration? = null
    private var sentListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null

    private val _state = MutableStateFlow(FriendsScreenState())
    val state: StateFlow<FriendsScreenState> = _state.asStateFlow()

    private val _friendToRemove = MutableStateFlow<Friend?>(null)
    val friendToRemove: StateFlow<Friend?> = _friendToRemove.asStateFlow()

    init {
        loadAllFriendsData()
    }

    private fun loadAllFriendsData() {
        if (currentUserId == null) {
            _state.update { it.copy(isLoading = false, errorMessage = "Usuário não autenticado.") }
            return
        }
        listenForReceivedRequests(currentUserId)
        listenForSentRequests(currentUserId)
        listenForFriends(currentUserId)
    }

    // ✅ ESTA É A FUNÇÃO QUE PROVAVELMENTE ESTÁ FALHANDO DEVIDO AO ÍNDICE
    private fun listenForReceivedRequests(myUserId: String) {
        receivedListener?.remove()
        receivedListener = db.collection("friend_requests")
            .whereEqualTo("receiverId", myUserId) // ⬅️ ESTA LINHA REQUER UM ÍNDICE
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // ❗️ PROCURE ESTE LOG NO SEU LOGCAT ❗️
                    Log.e("FriendsVM", "Erro ao ouvir pedidos recebidos", error)
                    _state.update { it.copy(errorMessage = "Erro ao carregar pedidos.") }
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
                _state.update { it.copy(receivedRequests = requests) }
            }
    }

    // Esta função funciona (como você disse)
    private fun listenForSentRequests(myUserId: String) {
        sentListener?.remove()
        sentListener = db.collection("friend_requests")
            .whereEqualTo("senderId", myUserId) // Esta query funciona (índice automático)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FriendsVM", "Erro ao ouvir pedidos enviados", error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
                _state.update { it.copy(sentRequests = requests) }
            }
    }

    private fun listenForFriends(myUserId: String) {
        friendsListener?.remove()
        friendsListener = db.collection("users").document(myUserId).collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FriendsVM", "Erro ao ouvir lista de amigos", error)
                    _state.update { it.copy(isLoading = false, errorMessage = "Erro ao carregar amigos.") }
                    return@addSnapshotListener
                }
                val friendsList = snapshot?.toObjects(Friend::class.java) ?: emptyList()
                _state.update { it.copy(isLoading = false, friends = friendsList) }
            }
    }

    // --- AÇÕES ---
    fun declineFriendRequest(request: FriendRequest) {
        if (currentUserId == null) return
        viewModelScope.launch {
            try {
                val requestDoc = db.collection("friend_requests")
                    .whereEqualTo("senderId", request.senderId)
                    .whereEqualTo("receiverId", request.receiverId)
                    .limit(1).get().await()

                if (!requestDoc.isEmpty) {
                    requestDoc.documents[0].reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e("FriendsVM", "Erro ao recusar pedido", e)
                _state.update { it.copy(errorMessage = "Erro ao recusar pedido.") }
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val myUserId = currentUserId; val senderId = request.senderId
        if (myUserId == null) return

        viewModelScope.launch {
            try {
                val myUserDoc = db.collection("users").document(myUserId).get().await()
                val myUser = myUserDoc.toObject(User::class.java)
                if (myUser == null) {
                    _state.update { it.copy(errorMessage = "Erro: Seus dados de usuário não foram encontrados.") }
                    return@launch
                }

                val friendForMe = Friend(uid = request.senderId, name = request.senderName, profilePictureUrl = request.senderPhotoUrl)
                val friendForSender = Friend(uid = myUser.uid, name = myUser.name, profilePictureUrl = myUser.profilePictureUrl)

                val myFriendDocRef = db.collection("users").document(myUserId).collection("friends").document(senderId)
                val senderFriendDocRef = db.collection("users").document(senderId).collection("friends").document(myUserId)
                val myUserDocRef = db.collection("users").document(myUserId)
                val senderUserDocRef = db.collection("users").document(senderId)

                val requestDocRef = db.collection("friend_requests")
                    .whereEqualTo("senderId", senderId)
                    .whereEqualTo("receiverId", myUserId)
                    .limit(1).get().await().documents.first().reference

                db.runTransaction { transaction ->
                    transaction.delete(requestDocRef)
                    transaction.set(myFriendDocRef, friendForMe)
                    transaction.set(senderFriendDocRef, friendForSender)
                    transaction.update(myUserDocRef, "friendCount", FieldValue.increment(1))
                    transaction.update(senderUserDocRef, "friendCount", FieldValue.increment(1))
                    null
                }.await()
            } catch (e: Exception) {
                Log.e("FriendsVM", "Erro ao aceitar pedido", e)
                _state.update { it.copy(errorMessage = "Erro ao aceitar pedido.") }
            }
        }
    }

    fun cancelSentRequest(request: FriendRequest) {
        if (currentUserId == null || currentUserId != request.senderId) return
        viewModelScope.launch {
            try {
                val requestDoc = db.collection("friend_requests")
                    .whereEqualTo("senderId", request.senderId)
                    .whereEqualTo("receiverId", request.receiverId)
                    .limit(1).get().await()

                if (!requestDoc.isEmpty) {
                    requestDoc.documents[0].reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e("FriendsVM", "Erro ao cancelar pedido", e)
                _state.update { it.copy(errorMessage = "Erro ao cancelar pedido.") }
            }
        }
    }

    fun requestRemoveFriend(friend: Friend) { _friendToRemove.value = friend }
    fun cancelRemoveFriend() { _friendToRemove.value = null }
    fun removeFriend(friend: Friend) {
        val myUserId = currentUserId; val friendId = friend.uid
        if (myUserId == null) return

        viewModelScope.launch {
            try {
                val batch = db.batch()
                val myFriendDocRef = db.collection("users").document(myUserId).collection("friends").document(friendId)
                val friendUserDocRef = db.collection("users").document(friendId).collection("friends").document(myUserId)
                val myUserDocRef = db.collection("users").document(myUserId)
                val friendUserDocRefMain = db.collection("users").document(friendId)
                batch.delete(myFriendDocRef)
                batch.delete(friendUserDocRef)
                batch.update(myUserDocRef, "friendCount", FieldValue.increment(-1))
                batch.update(friendUserDocRefMain, "friendCount", FieldValue.increment(-1))
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("FriendsVM", "Erro ao remover amigo", e)
                _state.update { it.copy(errorMessage = "Erro ao remover amigo.") }
            } finally {
                _friendToRemove.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        receivedListener?.remove()
        sentListener?.remove()
        friendsListener?.remove()
    }
}