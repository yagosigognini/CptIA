package br.com.CapitularIA.ui.features.club

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.CapitularIA.data.BookClub
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

// Reutilizando o enum do ProfileViewModel
enum class UpdateStatus { IDLE, LOADING, SUCCESS, ERROR }

class EditClubViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val _club = MutableLiveData<BookClub?>()
    val club: LiveData<BookClub?> = _club

    private val _updateStatus = MutableLiveData<UpdateStatus>(UpdateStatus.IDLE)
    val updateStatus: LiveData<UpdateStatus> = _updateStatus

    fun loadClub(clubId: String) {
        db.collection("clubs").document(clubId).get()
            .addOnSuccessListener { doc ->
                _club.value = doc.toObject(BookClub::class.java)
            }
            .addOnFailureListener {
                Log.e("EditClubVM", "Falha ao carregar clube", it)
            }
    }

    fun updateClub(
        name: String,
        description: String,
        isPublic: Boolean,
        maxMembers: Int,
        newImageUri: Uri?
    ) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.LOADING
            val currentClub = _club.value
            if (currentClub == null) {
                _updateStatus.value = UpdateStatus.ERROR
                return@launch
            }

            try {
                // Lógica de Upload (igual à de Criar Clube)
                val finalImageUrl = if (newImageUri != null) {
                    val imageRef = storage.reference.child("club_covers/${currentClub.id}.jpg")
                    imageRef.putFile(newImageUri).await()
                    imageRef.downloadUrl.await().toString()
                } else {
                    currentClub.imageUrl // Mantém a URL antiga
                }

                // Gera um novo código se o clube está sendo tornado privado
                val finalCode = if (!isPublic && currentClub.code == null) {
                    generateClubCode()
                } else if (isPublic) {
                    null // Clubes públicos não têm código
                } else {
                    currentClub.code // Mantém o código antigo
                }

                val updates = mapOf(
                    "name" to name,
                    "description" to description,
                    "public" to isPublic,
                    "maxMembers" to maxMembers,
                    "imageUrl" to finalImageUrl,
                    "code" to finalCode
                )

                db.collection("clubs").document(currentClub.id).update(updates).await()
                _updateStatus.value = UpdateStatus.SUCCESS
            } catch (e: Exception) {
                Log.e("EditClubVM", "Falha ao atualizar clube", e)
                _updateStatus.value = UpdateStatus.ERROR
            }
        }
    }

    private fun generateClubCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.IDLE
    }
}