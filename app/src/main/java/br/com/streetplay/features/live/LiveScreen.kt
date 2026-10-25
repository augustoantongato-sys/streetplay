package br.com.streetplay.features.live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveSource

@Composable
fun LiveScreen(
    source: LiveSource,
    channels: List<LiveChannel>,
    loading: Boolean,
    onSelectSource: (LiveSource) -> Unit,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = SPSpacing.xl).padding(top = SPSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SPSectionTitle("Canais ao vivo")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SPSpacing.sm), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(LiveSource.entries.toList()) { s -> SPButton(s.label, { onSelectSource(s) }, selected = s == source) }
        }
        SPText("Canais públicos do projeto IPTV-org. Alguns podem estar fora do ar ou bloqueados na sua região.", SPType.Caption)
        when {
            loading -> SPEmptyState("Carregando canais…", "Buscando a lista pública")
            channels.isEmpty() -> SPEmptyState("Nenhum canal disponível", "Verifique a internet e tente de novo.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(SPSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SPSpacing.md),
            ) {
                itemsIndexed(channels, key = { _, c -> c.id + c.url }) { i, c ->
                    SPFocusIndicator(
                        onClick = { onPlay(i) }, modifier = Modifier.height(104.dp),
                        container = SPColors.TileBase, focusedContainer = SPColors.TileFocused, idleBorder = SPColors.Border,
                    ) {
                        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            SPRemoteImage(c.logo, Modifier.size(54.dp), ContentScale.Fit) {
                                SPText(c.name.take(1).uppercase(), SPType.Title, Modifier.align(Alignment.Center), color = LocalAccent.current)
                            }
                            Spacer(Modifier.height(6.dp))
                            SPText(c.name, TextStyle(fontSize = 11.sp, color = SPColors.TextPrimary, textAlign = TextAlign.Center), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
