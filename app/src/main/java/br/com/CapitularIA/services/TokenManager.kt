package br.com.CapitularIA.services

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.tasks.await

/**
 * Pega o token FCM atual e o salva no Firestore (usando merge)
 * para o usuário logado.
 */
suspend fun updateFcmTokenForCurrentUser() {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val uid = auth.currentUser?.uid

    // Se o usuário não estiver logado, não faz nada.
    if (uid == null) {
        Log.w("TokenManager", "Usuário não logado, não é possível salvar o token.")
        return
    }

    try {
        // 1. Pega o token atual do dispositivo
        val token = Firebase.messaging.token.await()

        // 2. Cria um mapa com o token
        // (Nós usamos um campo "fcmToken", você pode mudar se quiser)
        val tokenData = mapOf("fcmToken" to token)

        // 3. Salva no Firestore
        // SetOptions.merge() é mais seguro: ele atualiza/cria o campo "fcmToken"
        // sem apagar os outros dados do usuário (nome, email, etc.)
        db.collection("users").document(uid)
            .set(tokenData, SetOptions.merge())
            .await()

        Log.d("TokenManager", "Token FCM atualizado no Firestore para o usuário: $uid")

    } catch (e: Exception) {
        Log.e("TokenManager", "Falha ao pegar ou salvar o token FCM.", e)
    }
}

/**
 * Salva um token específico no Firestore (usado pelo onNewToken).
 * Esta versão não é 'suspend' para poder ser chamada de qualquer lugar.
 */
fun saveSpecificToken(token: String) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val uid = auth.currentUser?.uid ?: return // Se não tiver user, não salva

    val tokenData = mapOf("fcmToken" to token)
    db.collection("users").document(uid)
        .set(tokenData, SetOptions.merge())
        .addOnSuccessListener { Log.d("TokenManager", "Token (novo) salvo no Firestore.") }
        .addOnFailureListener { e -> Log.w("TokenManager", "Falha ao salvar token (novo)", e) }
}