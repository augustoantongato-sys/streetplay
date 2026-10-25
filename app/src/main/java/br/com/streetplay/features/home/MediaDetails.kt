package br.com.streetplay.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.TrendingItem
import java.util.Locale

fun TrendingItem.meta(): String = buildList {
    add(typeLabel)
    if (year.isNotBlank()) add(year)
    if (rating > 0) add("★ " + String.format(Locale("pt", "BR"), "%.1f", rating))
}.joinToString("  ·  ")

@Composable
fun TrendingDetailsDialog(item: TrendingItem, onDismiss: () -> Unit) {
    SPDialog(item.title, onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SPRemoteImage(item.posterUrl, Modifier.width(92.dp).height(132.dp).clip(RoundedCornerShape(8.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SPText(item.meta(), SPType.Label, color = LocalAccent.current)
                SPText(item.overview.ifBlank { "Sinopse indisponível." }, TextStyle(fontSize = 14.sp, color = SPColors.TextPrimary), maxLines = 7)
            }
        }
        SPText("Dados e imagens: TMDB", SPType.Caption)
        SPButton("Fechar", onDismiss)
    }
}
