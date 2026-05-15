package br.com.CapitularIA.ui.features.home

import android.app.Application // ✅ NOVO
import android.content.Context // ✅ NOVO
import android.util.Log
import androidx.lifecycle.AndroidViewModel // ✅ MUDANÇA
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.BookClub
import br.com.CapitularIA.data.Quote // ✅ NOVO
import br.com.CapitularIA.data.User
import br.com.CapitularIA.data.allAppQuotes // ✅ NOVO
import br.com.CapitularIA.data.sampleQuote // ✅ NOVO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // ✅ NOVO
import java.util.Date // ✅ NOVO
import java.util.Locale // ✅ NOVO
import br.com.CapitularIA.services.updateFcmTokenForCurrentUser
import kotlin.collections.get
import kotlin.text.get

// ✅ MUDANÇA: Agora é um AndroidViewModel para ter acesso ao Contexto
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _user = MutableLiveData<User?>(null)
    val user: LiveData<User?> = _user

    private val _clubs = MutableLiveData<List<BookClub>>()
    val clubs: LiveData<List<BookClub>> = _clubs

    // ✅ NOVO: LiveData para a citação do dia
    private val _quoteOfTheDay = MutableLiveData<Quote>(sampleQuote) // Usa sampleQuote como padrão
    val quoteOfTheDay: LiveData<Quote> = _quoteOfTheDay

    // ✅ NOVO: Constantes para o SharedPreferences
    private val PREFS_NAME = "letraria_quote_prefs"
    private val KEY_LAST_DATE = "last_quote_date"
    private val KEY_LAST_QUOTE_INDEX = "last_quote_index"

    init {
        loadCurrentUser()
        loadQuoteOfTheDay() // ✅ NOVO: Carrega a citação ao iniciar
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            updateFcmTokenForCurrentUser()
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Log.e("HomeViewModel", "Nenhum usuário logado.")
                _user.value = null
                _clubs.value = emptyList()
                return@launch
            }

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        _user.value = document.toObject(User::class.java)
                        fetchUserClubs(uid)
                    }
                }
        }
    }

    private fun fetchUserClubs(userId: String) {
        db.collection("clubs").whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("HomeViewModel", "Erro ao buscar clubes do usuário.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val clubList = snapshot.toObjects(BookClub::class.java)
                    _clubs.value = clubList
                    Log.d("HomeViewModel", "Clubes do usuário carregados: ${clubList.size}")
                }
            }
    }

    // ✅ NOVO: Lógica principal da "Citação do Dia"
    private fun loadQuoteOfTheDay() {
        // Se a lista de citações estiver vazia, usa a citação de exemplo
        if (allAppQuotes.isEmpty()) {
            _quoteOfTheDay.value = sampleQuote
            return
        }

        // 1. Acessa o SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 2. Pega a data de hoje formatada (ex: "2025-10-24")
        val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 3. Pega a data e o índice salvos
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        val lastIndex = prefs.getInt(KEY_LAST_QUOTE_INDEX, -1)

        // 4. Compara as datas
        if (todayString == savedDate) {
            // É o mesmo dia. Apenas carrega a mesma citação.
            // Se o índice for inválido (ex: primeiro login), usa 0.
            val quoteIndex = if (lastIndex == -1) 0 else lastIndex
            _quoteOfTheDay.value = allAppQuotes[quoteIndex]

        } else {
            // É um novo dia!
            // 5. Calcula o novo índice (dá a volta na lista se chegar ao fim)
            val newIndex = (lastIndex + 1) % allAppQuotes.size
            val newQuote = allAppQuotes[newIndex]

            // 6. Atualiza o LiveData
            _quoteOfTheDay.value = newQuote

            // 7. Salva o novo índice e a data de hoje
            prefs.edit()
                .putString(KEY_LAST_DATE, todayString)
                .putInt(KEY_LAST_QUOTE_INDEX, newIndex)
                .apply()
        }
    }
}