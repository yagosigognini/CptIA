package br.com.CapitularIA.ui.features.club

import android.util.Log
import androidx.compose.runtime.State // ⬇️ NOVO
import androidx.compose.runtime.mutableStateOf // ⬇️ NOVO
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.BookItem // ⬇️ NOVO: Modelo da API
import br.com.CapitularIA.data.IndicatedBook // Mantemos para salvar
import br.com.CapitularIA.data.Message
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.getBestAvailableImageUrl // ⬇️ NOVO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration // ⬇️ NOVO
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ClubViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var currentClubId: String? = null // ⬇️ NOVO: Guarda o ID do clube atual

    // ⬇️ NOVO: Listeners para limpar depois
    private var clubListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var sortedUserListener: ListenerRegistration? = null

    private val _club = MutableLiveData<BookClub?>()
    val club: LiveData<BookClub?> = _club

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _sortedUser = MutableLiveData<User?>(null)
    val sortedUser: LiveData<User?> = _sortedUser

    // ⬇️ NOVO: Estado para o livro selecionado na busca, aguardando confirmação
    private val _bookPendingIndication = mutableStateOf<BookItem?>(null)
    val bookPendingIndication: State<BookItem?> = _bookPendingIndication
    // ⬆️ Fim do novo estado

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _accessDenied = MutableLiveData(false)
    val accessDenied: LiveData<Boolean> = _accessDenied

    private val _toastMessage = MutableLiveData<String?>(null)
    val toastMessage: LiveData<String?> = _toastMessage

    fun loadClubAndMessages(clubId: String) {
        _isLoading.value = true
        _accessDenied.value = false
        val currentUserId = auth.currentUser?.uid
        this.currentClubId = clubId // Guarda o ID

        // Limpa listeners antigos antes de registrar novos
        clubListener?.remove()
        messagesListener?.remove()
        sortedUserListener?.remove()

        clubListener = db.collection("clubs").document(clubId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("ClubViewModel", "Erro ao buscar clube $clubId", error)
                    _club.value = null
                    _messages.value = emptyList()
                    _sortedUser.value = null
                    _accessDenied.value = true
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val clubData = document.toObject(BookClub::class.java)
                    _club.value = clubData

                    if (clubData != null) {
                        // Verifica se usuário é membro
                        if (!clubData.members.contains(currentUserId)) {
                            _accessDenied.value = true
                            _isLoading.value = false
                            _messages.value = emptyList()
                            _sortedUser.value = null
                        } else {
                            // É membro, carrega o resto
                            _accessDenied.value = false
                            listenForMessages(clubId) // Inicia listener de mensagens
                            // Busca dados do usuário sorteado (se houver)
                            if (clubData.currentUserForCycleId != null) {
                                fetchSortedUserData(clubData.currentUserForCycleId)
                            } else {
                                _sortedUser.value = null // Ninguém sorteado
                            }
                            // Não definimos isLoading = false aqui, esperamos as mensagens
                        }
                    } else {
                        _isLoading.value = false // Falha ao converter dados do clube
                        _accessDenied.value = true
                        Log.w("ClubViewModel", "Dados do clube nulos após conversão para ID: $clubId")
                    }
                } else {
                    // Clube não encontrado
                    Log.w("ClubViewModel", "Documento do clube não encontrado para ID: $clubId")
                    _club.value = null
                    _messages.value = emptyList()
                    _sortedUser.value = null
                    _accessDenied.value = true
                    _isLoading.value = false
                }
            }
    }

    private fun listenForMessages(clubId: String) {
        messagesListener?.remove() // Limpa listener anterior
        messagesListener = db.collection("clubs").document(clubId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ClubViewModel", "Erro ao ouvir mensagens do clube $clubId.", error)
                    _messages.value = emptyList() // Limpa mensagens em caso de erro
                    // Considerar _isLoading.value = false aqui se for erro terminal?
                    return@addSnapshotListener
                }
                _messages.value = snapshot?.toObjects(Message::class.java) ?: emptyList()
                _isLoading.value = false // Define carregamento como concluído após receber mensagens (ou erro)
            }
    }

    private fun fetchSortedUserData(userId: String) {
        sortedUserListener?.remove() // Limpa listener anterior
        sortedUserListener = db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.w("ClubViewModel", "Erro ao buscar dados do usuário sorteado $userId.", error)
                    _sortedUser.value = null
                    return@addSnapshotListener
                }
                _sortedUser.value = document?.toObject(User::class.java)
            }
    }

    // ⬇️ --- NOVAS FUNÇÕES PARA INDICAÇÃO VIA BUSCA --- ⬇️

    /** Chamado pela Navegação quando um livro é retornado da BookSearchScreen */
    fun onBookSelectedFromSearch(bookItem: BookItem) {
        _bookPendingIndication.value = bookItem // Guarda o livro para confirmação
    }

    /** Chamado pelo diálogo de confirmação ao clicar em "Confirmar" */
    fun confirmBookIndication(cycleDaysInput: String) {
        val bookItem = _bookPendingIndication.value
        val clubId = currentClubId // Usa o ID do clube guardado
        val currentUserId = auth.currentUser?.uid

        // Validações básicas
        if (bookItem == null || clubId == null || currentUserId == null) {
            _toastMessage.value = "Erro: Não foi possível obter dados para indicar."
            _bookPendingIndication.value = null // Limpa o estado pendente
            return
        }
        val cycleDays = cycleDaysInput.toIntOrNull()
        if (cycleDays == null || cycleDays <= 0) {
            _toastMessage.value = "Por favor, insira um número válido de dias."
            // Não limpa o estado pendente, permite tentar de novo
            return
        }

        viewModelScope.launch {
            // Cria o objeto IndicatedBook (modelo que você já tem para salvar no clube)
            val indicatedBook = IndicatedBook(
                googleBookId = bookItem.id ?: "", // Adiciona o ID do Google se disponível
                title = bookItem.volumeInfo?.title ?: "Título Desconhecido",
                author = bookItem.volumeInfo?.authors?.joinToString(", ") ?: "Autor Desconhecido",
                publisher = bookItem.volumeInfo?.publisher ?: "",
                coverUrl = bookItem.volumeInfo?.imageLinks.getBestAvailableImageUrl() ?: "" // Pega a melhor URL
            )

            // Calcula a data final
            val cycleDaysInMillis = TimeUnit.DAYS.toMillis(cycleDays.toLong())
            val endDate = System.currentTimeMillis() + cycleDaysInMillis

            // Prepara os dados para atualizar no Firestore
            val clubUpdates = mapOf(
                "indicatedBook" to indicatedBook, // Salva o objeto IndicatedBook
                "cycleEndDate" to endDate,
                "readingCycleDays" to cycleDays // Atualiza a preferência
            )

            try {
                // Atualiza o documento do clube
                db.collection("clubs").document(clubId).update(clubUpdates).await()
                // Posta a mensagem de sistema no chat
                postIndicationMessage(clubId, indicatedBook, currentUserId)
                _toastMessage.value = "Livro indicado com sucesso!"
                _bookPendingIndication.value = null // Limpa o estado pendente após sucesso
            } catch (e: Exception) {
                Log.e("ClubViewModel", "Erro ao confirmar indicação no Firestore", e)
                _toastMessage.value = "Erro ao salvar a indicação."
                _bookPendingIndication.value = null // Limpa o estado pendente no erro também
            }
        }
    }

    /** Chamado pelo diálogo de confirmação ao clicar em "Cancelar" ou fechar */
    fun cancelBookIndication() {
        _bookPendingIndication.value = null // Apenas limpa o estado
    }

    // ⬆️ --- FIM DAS NOVAS FUNÇÕES --- ⬆️

    // ❌ REMOVIDA: A função antiga indicateBook foi removida
    // fun indicateBook(...) { ... }


    fun drawUserForCycle() {
        viewModelScope.launch {
            val clubId = currentClubId // Usa o ID guardado
            val currentClub = _club.value
            val adminId = auth.currentUser?.uid
            if (clubId == null || currentClub == null || adminId == null) {
                _toastMessage.value = "Erro: Dados do clube/usuário indisponíveis."
                return@launch
            }
            if (currentClub.adminId != adminId) {
                _toastMessage.value = "Apenas o admin pode realizar o sorteio."
                return@launch
            }
            if (currentClub.members.isEmpty()) {
                _toastMessage.value = "Não há membros no clube para sortear."
                return@launch
            }

            // Lógica simples de sorteio (pode ser melhorada para evitar repetir)
            val randomUserId = currentClub.members.random()

            // Prepara atualização para resetar o ciclo
            val updates = mapOf(
                "currentUserForCycleId" to randomUserId,
                "indicatedBook" to null,
                "cycleEndDate" to null
            )

            try {
                db.collection("clubs").document(clubId).update(updates).await()
                _toastMessage.value = "Novo usuário sorteado!"
                // Opcional: Postar mensagem de sistema sobre o sorteio
            } catch (e: Exception) {
                Log.e("ClubViewModel", "Erro ao sortear usuário no Firestore", e)
                _toastMessage.value = "Erro ao tentar sortear usuário."
            }
        }
    }

    fun sendMessage(text: String, clubId: String) {
        // (Sem mudanças necessárias aqui)
        if (text.isBlank()) return
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)

                if (user != null) {
                    val message = Message(
                        text = text,
                        senderId = user.uid,
                        senderName = user.name,
                        senderPhotoUrl = user.profilePictureUrl,
                        type = "USER"
                    )
                    db.collection("clubs").document(clubId).collection("messages").add(message).await()
                } else {
                    Log.w("ClubViewModel", "Não foi possível buscar dados do usuário para enviar mensagem.")
                }
            } catch (e: Exception) { Log.e("ClubViewModel", "Erro ao enviar mensagem", e) }
        }
    }

    private suspend fun postIndicationMessage(clubId: String, book: IndicatedBook, senderId: String) {
        // (Melhoria: Adicionar try-catch aqui também)
        try {
            val userDoc = db.collection("users").document(senderId).get().await()
            val senderName = userDoc.getString("name") ?: "Usuário" // Nome padrão

            val indicationMessage = Message(
                senderId = senderId,
                senderName = senderName, // Nome de quem indicou
                type = "INDICATION", // Tipo especial de mensagem
                indicatedBook = book, // Anexa os dados do livro
                text = "$senderName indicou: ${book.title}" // Texto descritivo
            )
            db.collection("clubs").document(clubId).collection("messages").add(indicationMessage).await()
        } catch (e: Exception) {
            Log.e("ClubViewModel", "Erro ao postar mensagem de indicação no chat", e)
            // Considerar mostrar um erro para o usuário? (_toastMessage.value = ...)
        }
    }

    fun onToastMessageShown() {
        _toastMessage.value = null
    }

    // ⬇️ NOVO: Limpa todos os listeners quando o ViewModel é destruído
    override fun onCleared() {
        super.onCleared()
        clubListener?.remove()
        messagesListener?.remove()
        sortedUserListener?.remove()
        Log.d("ClubViewModel", "Listeners do Firestore removidos.")
    }
}