package br.com.CapitularIA.ui.features.profile



import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.BookItem
import br.com.CapitularIA.data.ProfileRatedBook
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.getBestAvailableImageUrl
import br.com.CapitularIA.data.FriendRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

enum class UpdateStatus { IDLE, LOADING, SUCCESS, ERROR }

enum class FriendshipStatus {
    LOADING,          // Checando...
    SELF,             // É o seu próprio perfil
    NOT_FRIENDS,      // Não são amigos
    REQUEST_SENT,     // Você enviou um pedido
    REQUEST_RECEIVED, // Você recebeu um pedido
    FRIENDS           // Já são amigos
}

class ProfileViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    // Listeners
    private var clubsListener: ListenerRegistration? = null
    private var ratedBooksListener: ListenerRegistration? = null
    private var friendStatusListener: ListenerRegistration? = null // ⬇️ NOVO: Listener para amizades

    // LiveData e State para a UI
    private val _user = MutableLiveData<User?>(null)
    val user: LiveData<User?> = _user

    private val _clubs = MutableLiveData<List<BookClub>>(emptyList())
    val clubs: LiveData<List<BookClub>> = _clubs

    private val _ratedBooks = MutableLiveData<List<ProfileRatedBook>>(emptyList())
    val ratedBooks: LiveData<List<ProfileRatedBook>> = _ratedBooks

    private val _isOwnProfile = MutableLiveData<Boolean>()
    val isOwnProfile: LiveData<Boolean> = _isOwnProfile

    // ⬇️ NOVO: LiveData para o Status de Amizade
    private val _friendshipStatus = MutableLiveData<FriendshipStatus>(FriendshipStatus.LOADING)
    val friendshipStatus: LiveData<FriendshipStatus> = _friendshipStatus
    // ⬆️ FIM DO NOVO LiveData

    private val _updateStatus = MutableLiveData<UpdateStatus>(UpdateStatus.IDLE)
    val updateStatus: LiveData<UpdateStatus> = _updateStatus

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _toastMessage = MutableLiveData<String?>(null)
    val toastMessage: LiveData<String?> = _toastMessage

    // State para diálogo de avaliação
    private val _bookToRate = mutableStateOf<BookItem?>(null)
    val bookToRate: State<BookItem?> = _bookToRate

    // State para diálogo de exclusão
    private val _bookToDelete = mutableStateOf<ProfileRatedBook?>(null)
    val bookToDelete: State<ProfileRatedBook?> = _bookToDelete

    private val _friendToRemove = mutableStateOf<User?>(null)
    val friendToRemove: State<User?> = _friendToRemove

    // --- Funções ---

    // ... (loadUserProfile, fetchRatedBooks, fetchUserClubs - sem mudanças na lógica interna, mas limpam listeners) ...
    fun loadUserProfile(userId: String?) {
        val currentUserId = auth.currentUser?.uid
        val idToFetch = userId ?: currentUserId

        if (idToFetch == null) {
            Log.e("ProfileViewModel", "ID do usuário para carregar perfil é nulo.")
            _user.value = null
            _clubs.value = emptyList()
            _ratedBooks.value = emptyList()
            _isOwnProfile.value = false
            clubsListener?.remove()
            ratedBooksListener?.remove()
            friendStatusListener?.remove()
            return
        }

        _isOwnProfile.value = (idToFetch == currentUserId)
        clubsListener?.remove()
        ratedBooksListener?.remove()
        friendStatusListener?.remove()

        val isOwn = (idToFetch == currentUserId)
        _isOwnProfile.value = isOwn

        if (isOwn) {
            // Se é o perfil próprio, define o status e não precisa checar amizade
            _friendshipStatus.value = FriendshipStatus.SELF
        } else {
            // Se é perfil de outro, começa a checar o status
            _friendshipStatus.value = FriendshipStatus.LOADING
            checkFriendshipStatus(viewerId = currentUserId!!, ownerId = idToFetch)
        }

        db.collection("users").document(idToFetch).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userObject = document.toObject(User::class.java)
                    _user.value = userObject
                    if (userObject != null) {
                        fetchUserClubs(userObject.uid, currentUserId)
                        fetchRatedBooks(userObject.uid)
                    } else {
                        Log.w("ProfileViewModel", "Falha ao converter documento do usuário. ID: $idToFetch")
                        _user.value = null; _clubs.value = emptyList(); _ratedBooks.value = emptyList()
                    }
                } else {
                    Log.w("ProfileViewModel", "Documento do usuário não encontrado. ID: $idToFetch")
                    _user.value = null; _clubs.value = emptyList(); _ratedBooks.value = emptyList()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Falha ao buscar usuário $idToFetch", e)
                _user.value = null; _clubs.value = emptyList(); _ratedBooks.value = emptyList()
            }
    }

    private fun checkFriendshipStatus(viewerId: String, ownerId: String) {
        // Esta função precisa checar 3 coisas em sequência:
        // 1. Já são amigos?
        // 2. Eu enviei um pedido?
        // 3. Eu recebi um pedido?

        viewModelScope.launch {
            try {
                // 1. Checa se já são amigos (na subcoleção do visualizador)
                val friendDoc = db.collection("users").document(viewerId)
                    .collection("friends").document(ownerId).get().await()
                if (friendDoc.exists()) {
                    _friendshipStatus.value = FriendshipStatus.FRIENDS
                    Log.d("ProfileVM", "Status Amizade: AMIGOS")
                    return@launch
                }

                // 2. Checa se o visualizador (viewer) enviou um pedido para o dono (owner)
                // Usamos addSnapshotListener para atualizar a UI se o pedido for cancelado em outro lugar
                friendStatusListener = db.collection("friend_requests")
                    .whereEqualTo("senderId", viewerId)
                    .whereEqualTo("receiverId", ownerId)
                    .limit(1)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ProfileVM", "Erro ao checar pedido enviado", error)
                            _friendshipStatus.value = FriendshipStatus.NOT_FRIENDS // Default em caso de erro
                            return@addSnapshotListener
                        }

                        if (snapshot != null && !snapshot.isEmpty) {
                            // Pedido enviado existe
                            _friendshipStatus.value = FriendshipStatus.REQUEST_SENT
                            Log.d("ProfileVM", "Status Amizade: PEDIDO ENVIADO")
                        } else {
                            // Pedido enviado não existe, checa se recebeu um
                            checkReceivedFriendRequest(viewerId = viewerId, ownerId = ownerId)
                        }
                    }

            } catch (e: Exception) {
                Log.e("ProfileVM", "Erro ao checar status de amizade (Passo 1)", e)
                _friendshipStatus.value = FriendshipStatus.NOT_FRIENDS // Default em caso de erro
            }
        }
    }

    private fun checkReceivedFriendRequest(viewerId: String, ownerId: String) {
        // Limpa listener anterior (se houver)
        friendStatusListener?.remove()

        // 3. Checa se o visualizador (viewer) recebeu um pedido do dono (owner)
        friendStatusListener = db.collection("friend_requests")
            .whereEqualTo("senderId", ownerId)
            .whereEqualTo("receiverId", viewerId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileVM", "Erro ao checar pedido recebido", error)
                    _friendshipStatus.value = FriendshipStatus.NOT_FRIENDS
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Pedido recebido existe
                    _friendshipStatus.value = FriendshipStatus.REQUEST_RECEIVED
                    Log.d("ProfileVM", "Status Amizade: PEDIDO RECEBIDO")
                } else {
                    // 4. Se não é amigo, não enviou e não recebeu: Não são amigos
                    _friendshipStatus.value = FriendshipStatus.NOT_FRIENDS
                    Log.d("ProfileVM", "Status Amizade: NÃO AMIGOS")
                }
            }
    }

    fun sendFriendRequest() {
        viewModelScope.launch {
            val ownerUser = _user.value // O usuário do perfil que estamos vendo
            val viewerId = auth.currentUser?.uid // O usuário logado

            if (ownerUser == null || viewerId == null) {
                _errorMessage.value = "Erro: Usuário não encontrado."
                return@launch
            }

            // Garante que não está enviando para si mesmo ou que já não há uma relação
            if (ownerUser.uid == viewerId || friendshipStatus.value != FriendshipStatus.NOT_FRIENDS) {
                _errorMessage.value = "Não é possível enviar pedido."
                return@launch
            }

            // Precisamos dos dados do *visualizador* (quem está enviando)
            val viewerDoc = db.collection("users").document(viewerId).get().await()
            val viewerUser = viewerDoc.toObject(User::class.java)
            if (viewerUser == null) {
                _errorMessage.value = "Erro: Seus dados não foram encontrados."
                return@launch
            }

            // Cria o objeto FriendRequest
            val request = FriendRequest(
                senderId = viewerUser.uid,
                senderName = viewerUser.name,
                senderPhotoUrl = viewerUser.profilePictureUrl,
                receiverId = ownerUser.uid,
                receiverName = ownerUser.name,
                receiverPhotoUrl = ownerUser.profilePictureUrl
            )

            try {
                // Salva na coleção 'friend_requests'
                db.collection("friend_requests").add(request).await()

                // Atualiza o estado da UI (o listener fará isso, mas forçamos para ser instantâneo)
                _friendshipStatus.value = FriendshipStatus.REQUEST_SENT
                _toastMessage.value = "Pedido de amizade enviado!"
            } catch (e: Exception) {
                Log.e("ProfileVM", "Erro ao enviar pedido de amizade", e)
                _errorMessage.value = "Erro ao enviar pedido."
            }
        }
    }

    /** Chamado pelo botão "Amigos" no perfil para iniciar a remoção */
    fun requestRemoveFriend() {
        if (_isOwnProfile.value == false) { // Só pode remover se for perfil de outro
            _friendToRemove.value = _user.value // Pega o usuário do perfil atual
        }
    }

    /** Chamado pelo diálogo para cancelar a remoção */
    fun cancelRemoveFriend() {
        _friendToRemove.value = null
    }

    /** Remove um amigo (chamado pelo diálogo de confirmação) */
    fun confirmRemoveFriend() {
        val myUserId = auth.currentUser?.uid
        val friendToRemove = _friendToRemove.value // Pega o amigo pendente

        if (myUserId == null || friendToRemove == null) {
            _errorMessage.value = "Erro ao remover amigo."
            _friendToRemove.value = null
            return
        }
        val friendId = friendToRemove.uid

        viewModelScope.launch {
            try {
                // Lógica de Lote (Batch) copiada do FriendsViewModel
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
                Log.d("ProfileVM", "Amizade com ${friendToRemove.name} removida.")
                _toastMessage.value = "Amizade removida."
                _friendshipStatus.value = FriendshipStatus.NOT_FRIENDS // Atualiza a UI

            } catch (e: Exception) {
                Log.e("ProfileVM", "Erro ao remover amigo", e)
                _errorMessage.value = "Erro ao remover amigo."
            } finally {
                _friendToRemove.value = null // Fecha o diálogo
            }
        }
    }

    private fun fetchRatedBooks(userId: String) {
        val query = db.collection("users").document(userId).collection("rated_books")
            .orderBy("ratedAt", Query.Direction.DESCENDING)
        ratedBooksListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("ProfileViewModel", "Erro ao buscar livros avaliados para $userId.", error)
                _ratedBooks.value = emptyList()
                return@addSnapshotListener
            }
            _ratedBooks.value = snapshot?.toObjects(ProfileRatedBook::class.java) ?: emptyList()
            Log.d("ProfileViewModel", "Livros avaliados atualizados: ${_ratedBooks.value?.size ?: 0}")
        }
    }

    private fun fetchUserClubs(profileOwnerId: String, viewerId: String?) {
        clubsListener = db.collection("clubs").whereArrayContains("members", profileOwnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ProfileViewModel", "Erro ao buscar clubes para $profileOwnerId.", error)
                    _clubs.value = emptyList()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allClubs = snapshot.toObjects(BookClub::class.java)
                    _clubs.value = allClubs.filter { club ->
                        club.isPublic || club.members.contains(viewerId)
                    }
                } else {
                    _clubs.value = emptyList()
                }
            }
    }

    // --- Funções de Avaliação ---
    fun onBookSelectedForRating(book: BookItem) { _bookToRate.value = book }
    fun onRatingDialogDismiss() { _bookToRate.value = null }
    fun onRatingSubmitted(rating: Float) {
        val book = _bookToRate.value ?: return
        val uid = auth.currentUser?.uid ?: return

        // ... (criação do ratedBookData - sem mudanças) ...
        val ratedBookData = mapOf(
            "googleBookId" to book.id,
            "title" to book.volumeInfo?.title,
            "authors" to book.volumeInfo?.authors,
            "coverUrl" to book.volumeInfo?.imageLinks.getBestAvailableImageUrl(),
            "rating" to rating,
            "ratedAt" to FieldValue.serverTimestamp()
        )

        // ... (chamada ao Firestore add() - sem mudanças) ...
        db.collection("users").document(uid).collection("rated_books")
            .add(ratedBookData)
            .addOnSuccessListener { Log.d("ProfileVM", "Livro avaliado salvo: ${book.volumeInfo?.title}") }
            .addOnFailureListener { e -> Log.e("ProfileVM", "Erro ao salvar avaliação", e); _errorMessage.value = "Erro ao salvar avaliação." }
        _bookToRate.value = null
    }

    // --- Funções de Exclusão ---
    fun requestDeleteBook(book: ProfileRatedBook) { _bookToDelete.value = book }
    fun cancelDeleteBook() { _bookToDelete.value = null }
    fun confirmDeleteBook() {
        val book = _bookToDelete.value
        val userId = auth.currentUser?.uid
        if (book == null || userId == null || book.id.isBlank()) { /* ... (erro) ... */ return }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("rated_books").document(book.id)
                    .delete()
                    .await()
                Log.d("ProfileVM", "Livro '${book.title}' excluído.")
                _toastMessage.value = "Livro removido da estante." // ✅ USA O TOAST AQUI
                _bookToDelete.value = null
            } catch (e: Exception) {
                Log.e("ProfileVM", "Erro ao excluir livro ${book.id}", e)
                _errorMessage.value = "Erro ao remover livro."
                _bookToDelete.value = null
            }
        }
    }

    // --- Funções de Atualização de Perfil ---
    fun updateUserProfile(name: String, aboutMe: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.LOADING
            val uid = auth.currentUser?.uid ?: run { /* ... (tratamento de erro) ... */ return@launch }
            try {
                // Lógica de upload da imagem (se houver nova)
                val imageUrl = if (newImageUri != null) {
                    val imageRef = storage.reference.child("profile_pictures/$uid.jpg")
                    imageRef.putFile(newImageUri).await()
                    imageRef.downloadUrl.await().toString()
                } else {
                    user.value?.profilePictureUrl // Mantém a URL antiga se não houver nova imagem
                }

                // Atualiza o documento no Firestore
                updateUserDocument(uid, name, aboutMe, imageUrl)

            } catch (e: Exception) { /* ... (tratamento de erro) ... */ }
        }
    }

    private suspend fun updateUserDocument(uid: String, name: String, aboutMe: String, imageUrl: String?) {
        val userRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>()
        updates["name"] = name
        updates["name_lowercase"] = name.lowercase(Locale.ROOT)
        updates["aboutMe"] = aboutMe
        imageUrl?.let { updates["profilePictureUrl"] = it } // Adiciona URL apenas se não for nula

        try {
            userRef.update(updates).await()
            _updateStatus.value = UpdateStatus.SUCCESS
            loadUserProfile(uid) // Recarrega para mostrar dados atualizados
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Erro ao atualizar documento do usuário $uid", e)
            _updateStatus.value = UpdateStatus.ERROR
            _errorMessage.value = "Falha ao salvar alterações."
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.IDLE
        _errorMessage.value = null
    }


    // ⬇️ --- ADICIONADO: Função para resetar o Toast --- ⬇️

    /** Chamado pela UI depois que a mensagem Toast foi exibida */
    fun onToastMessageShown() {
        _toastMessage.value = null
    }
    // ⬆️ --- FIM DA ADIÇÃO --- ⬆️



    // Limpa listeners ao destruir ViewModel
    override fun onCleared() {
        super.onCleared()
        clubsListener?.remove()
        ratedBooksListener?.remove()
        friendStatusListener?.remove()
        Log.d("ProfileViewModel", "ViewModel cleared, listeners removidos.")
    }
}