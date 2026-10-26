package br.com.streetplay.features.home

import androidx.compose.runtime.Composable
import br.com.streetplay.core.design.SPButton
import br.com.streetplay.core.design.SPDialog
import br.com.streetplay.domain.HomeEntry

/** Menu de um app da Home (segure OK): abrir, remover e mudar de lugar. */
@Composable
fun HomeEntryDialog(
    entry: HomeEntry, canMoveBefore: Boolean, canMoveAfter: Boolean,
    onOpen: () -> Unit, onRemove: () -> Unit, onMove: (Int) -> Unit, onDismiss: () -> Unit,
) {
    SPDialog(entry.label, onDismiss) {
        SPButton(if (entry.installed) "Abrir" else "Procurar na loja", { onOpen(); onDismiss() })
        SPButton("Remover da tela inicial", { onRemove(); onDismiss() })
        if (canMoveBefore) SPButton("Mover para antes", { onMove(-1) })
        if (canMoveAfter) SPButton("Mover para depois", { onMove(1) })
        SPButton("Fechar", onDismiss)
    }
}
