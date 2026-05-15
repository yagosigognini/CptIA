package br.com.CapitularIA.ui.features.profile // Ou br.com.CapitularIA.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import br.com.CapitularIA.ui.theme.CapitularIATheme
import java.util.Locale // Import para formatar o float

@Composable
fun RatingDialog(
    bookTitle: String?, // Pode ser nulo se o bookItem.volumeInfo for nulo
    onDismiss: () -> Unit,
    onSubmit: (Float) -> Unit
) {
    // Estado interno para a nota selecionada no slider
    var rating by remember { mutableFloatStateOf(3f) } // Nota inicial 3.0

    AlertDialog(
        onDismissRequest = onDismiss, // Chama onDismiss se clicar fora ou no botão voltar
        title = { Text("Avaliar Livro") },
        text = {
            Column {
                // Mostra o título do livro, ou uma mensagem padrão se for nulo
                Text("Qual nota você dá para \"${bookTitle ?: "este livro"}\"?")
                Slider(
                    value = rating,
                    onValueChange = { rating = it }, // Atualiza o estado da nota
                    valueRange = 0f..5f, // Range de 0 a 5 estrelas
                    steps = 9 // Permite notas de meio em meio (0, 0.5, 1, 1.5, ... 5.0)
                )
                Text(
                    // Formata a nota para uma casa decimal (ex: "3.5")
                    text = "Nota: ${"%.1f".format(Locale.US, rating)}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating) }) { // Chama onSubmit com a nota final
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { // Chama onDismiss se clicar em Cancelar
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String, // Título (ex: "Remover Livro" ou "Remover Amigo")
    text: String,  // Texto de confirmação
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remover", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Preview para testar o diálogo isoladamente
@Preview
@Composable
fun RatingDialogPreview() {
    CapitularIATheme {
        RatingDialog(
            bookTitle = "O Nome do Vento",
            onDismiss = {},
            onSubmit = {}
        )
    }
}