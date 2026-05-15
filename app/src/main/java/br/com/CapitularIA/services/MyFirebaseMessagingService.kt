package br.com.CapitularIA.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri // ✅ NOVO IMPORT
import br.com.CapitularIA.MainActivity
import br.com.CapitularIA.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.media.RingtoneManager
import android.media.AudioAttributes
import br.com.CapitularIA.services.saveSpecificToken

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "MyFirebaseMsgService"

    /**
     * Chamado quando uma nova mensagem é recebida.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(tag, "Mensagem recebida de: ${remoteMessage.from}")

        val notification = remoteMessage.notification
        val data = remoteMessage.data // ✅ Pega o payload de DADOS

        // Pega o título e corpo da notificação (se houver)
        val title = notification?.title
        val body = notification?.body

        // ✅ Pega o nosso link direto (URI) do payload de DADOS
        // O nome da chave ("deep_link_uri") deve ser EXATAMENTE o mesmo
        // que vamos usar na Cloud Function.
        val deepLinkUri = data["deep_link_uri"]

        Log.d(tag, "Notificação recebida: Title='$title', Body='$body', DeepLink='$deepLinkUri'")

        // ✅ Chama o sendNotification passando o deepLinkUri
        sendNotification(title, body, deepLinkUri)
    }

    /**
     * Chamado quando o FCM gera um novo token para o dispositivo.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "Novo Token FCM gerado: $token")
        // Salva o token no Firestore
        saveSpecificToken(token)
    }


    /**
     * Cria e exibe uma notificação que, ao ser clicada,
     * abre um Deep Link (URI) específico.
     */
    // ✅ MUDANÇA NA ASSINATURA: Adiciona 'deepLinkUri'
    private fun sendNotification(title: String?, messageBody: String?, deepLinkUri: String?) {
        val channelId = "letraria_default_channel" // ID do canal
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // --- Criação do Canal (Obrigatório no Android 8.0+) ---
        // (Sem mudanças aqui)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificações Gerais", // Nome do canal visível para o usuário
                NotificationManager.IMPORTANCE_HIGH
            )
            // Você pode adicionar mais configurações ao canal aqui (som, vibração, etc.)
            // ✅ --- ADICIONE ESTAS LINHAS PARA O SOM ---
            // Pega o som de notificação padrão do celular
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            // Define como o áudio deve ser reproduzido
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            // Aplica o som e os atributos ao canal
            channel.setSound(soundUri, audioAttributes)
            // ✅ --- FIM DAS LINHAS DE SOM ---

            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            notificationManager.createNotificationChannel(channel)
        }
        // --- Fim da Criação do Canal ---

        // --- ✅ CRIAÇÃO DO PENDINGINTENT (A MÁGICA DO DEEP LINK) ---
        val pendingIntent: PendingIntent = if (deepLinkUri != null && deepLinkUri.isNotBlank()) {
            // Se recebemos um link válido...
            Log.d(tag, "Criando PendingIntent com Deep Link para: $deepLinkUri")
            try {
                // 1. Cria um Intent normal que aponta para a MainActivity
                val intent = Intent(
                    Intent.ACTION_VIEW,     // Ação para visualizar dados (nosso deep link)
                    deepLinkUri.toUri(),    // O URI do deep link como 'data' do Intent
                    this,                   // Contexto
                    MainActivity::class.java // A Activity que vai receber o Intent
                )
                // Flags importantes para a navegação funcionar corretamente
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // 2. Cria o PendingIntent a partir desse Intent
                PendingIntent.getActivity(
                    this,
                    0, // Request code (pode ser qualquer número)
                    intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // Flags (IMMUTABLE é importante)
                )
            } catch (e: Exception) {
                // Fallback se o URI for inválido ou outra coisa der errado
                Log.e(tag, "Erro ao criar PendingIntent com Deep Link URI: $deepLinkUri", e)
                createDefaultPendingIntent() // Usa o intent padrão de abrir o app
            }
        } else {
            // Se não recebemos um link, só abre o app na tela inicial (comportamento antigo)
            Log.d(tag, "Criando PendingIntent padrão (sem Deep Link).")
            createDefaultPendingIntent()
        }
        // --- FIM DA CRIAÇÃO DO PENDINGINTENT ---

        // Constrói a notificação (igual a antes, mas usa o novo pendingIntent)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name) // Ícone obrigatório
            .setContentTitle(title ?: getString(R.string.app_name)) // Usa o nome do app se não tiver título
            .setContentText(messageBody)
            .setAutoCancel(true) // Notificação some ao ser clicada
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Importância alta (aparece na tela)
            .setContentIntent(pendingIntent) // ✅ Usa nosso novo PendingIntent inteligente

        // Mostra a notificação
        // ✅ USA UM ID ÚNICO (baseado no tempo) para que notificações não se sobrescrevam
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Função auxiliar para criar o PendingIntent padrão (apenas abre o app).
     */
    private fun createDefaultPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Limpa a pilha de telas
        return PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // Flag IMMUTABLE é importante
        )
    }
}