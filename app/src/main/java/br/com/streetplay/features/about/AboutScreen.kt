package br.com.streetplay.features.about

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.streetplay.core.design.*

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }
    Column(modifier.fillMaxSize().padding(SPSpacing.xl), verticalArrangement = Arrangement.spacedBy(SPSpacing.md)) {
        SPSectionTitle("Sobre")
        SPLogo(size = 72)
        SPText("Seu entretenimento. Do seu jeito.", SPType.Title, color = SPColors.TextSecondary)
        SPCard {
            SPText("Versão $version", SPType.Body)
            SPText("Launcher para Android TV, Google TV e TV Boxes Android.", SPType.Body)
            SPText("Sem anúncios · sem rastreadores · sem coleta de dados · funciona offline.", SPType.Caption)
            SPText("Kotlin · Jetpack Compose for TV · Room · DataStore", SPType.Caption)
        }
    }
}
