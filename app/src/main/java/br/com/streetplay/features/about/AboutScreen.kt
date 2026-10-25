package br.com.streetplay.features.about

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.design.*

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }
    Column(modifier.fillMaxSize().padding(SPSpacing.xl), verticalArrangement = Arrangement.spacedBy(SPSpacing.md)) {
        SPSectionTitle("Sobre")
        SPLogo(Modifier.height(90.dp))
        SPText("Seu entretenimento. Do seu jeito.", SPType.Title, color = SPColors.TextSecondary)
        SPCard {
            SPText("Versão $version", SPType.Body)
            SPText("Launcher para Android TV, Google TV e TV Boxes Android.", SPType.Body)
            SPText("Sem anúncios · sem rastreadores · não coleta dados pessoais.", SPType.Caption)
            SPText("Filmes e séries em destaque: dados e imagens do TMDB. Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB.", SPType.Caption)
            SPText("Canais ao vivo: listas públicas do projeto IPTV-org. A disponibilidade depende de cada emissora.", SPType.Caption)
            SPText("Kotlin · Jetpack Compose for TV · Room · DataStore", SPType.Caption)
        }
    }
}
