package br.com.CapitularIA.ui.features.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.CapitularIA.data.BookClub
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchClubViewModel : ViewModel() {

    private val db = Firebase.firestore

    private val _clubs = MutableLiveData<List<BookClub>>()
    val clubs: LiveData<List<BookClub>> = _clubs

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        fetchPublicClubs()
    }

    private fun fetchPublicClubs() {
        _isLoading.value = true
        // Criamos uma query que busca na coleção "clubs" APENAS os documentos
        // onde o campo "public" é igual a 'true'.
        db.collection("clubs").whereEqualTo("public", true)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.w("SearchClubViewModel", "Erro ao buscar clubes.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Mapeamos os documentos recebidos para a nossa data class BookClub
                    val clubList = snapshot.toObjects(BookClub::class.java)
                    _clubs.value = clubList
                    Log.d("SearchClubViewModel", "Clubes públicos encontrados: ${clubList.size}")
                }
            }
    }
}