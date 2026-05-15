package br.com.CapitularIA.ui.features.club // Ou seu pacote de componentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.CapitularIA.ui.theme.CapitularIATheme

@Composable
fun IndicationConfirmationDialog(
    bookTitle: String?, // Título do livro selecionado
    initialCycleDays: String, // Valor inicial (do clube ou padrão)
    onDismiss: () -> Unit, // Chamado ao cancelar/fechar
    onConfirm: (String) -> Unit // Chamado ao confirmar, envia os dias como String
) {
    // Estado interno para guardar o valor digitado dos dias
    var cycleDays by remember { mutableStateOf(initialCycleDays) }
    // Estado para verificar se a entrada é válida (não vazia e maior que zero)
    val isValidInput = remember(cycleDays) {
        cycleDays.isNotBlank() && (cycleDays.toIntOrNull() ?: 0) > 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Indicação") },
        text = {
            Column {
                // Mostra o título do livro selecionado
                Text("Você selecionou \"${bookTitle ?: "este livro"}\".")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Defina o tempo de leitura para este ciclo:")
                Spacer(modifier = Modifier.height(8.dp))
                // Campo de texto para os dias
                OutlinedTextField(
                    value = cycleDays,
                    onValueChange = { newValue ->
                        // Permite apenas dígitos e limita o tamanho (ex: 3 dígitos)
                        cycleDays = newValue.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Dias para leitura") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Teclado numérico
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isValidInput && cycleDays.isNotEmpty() // Mostra erro se não for válido e não estiver vazio
                )
                if (!isValidInput && cycleDays.isNotEmpty()) {
                    Text(
                        "Por favor, insira um número maior que 0.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(cycleDays) }, // Chama onConfirm com o valor digitado
                enabled = isValidInput // Habilita o botão apenas se a entrada for válida
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { // Chama onDismiss
                Text("Cancelar")
            }
        }
    )
}

// Preview para testar o diálogo isoladamente
@Preview
@Composable
fun IndicationConfirmationDialogPreview() {
    CapitularIATheme {
        IndicationConfirmationDialog(
            bookTitle = "A Guerra dos Tronos",
            initialCycleDays = "21",
            onDismiss = {},
            onConfirm = {}
        )
    }
}