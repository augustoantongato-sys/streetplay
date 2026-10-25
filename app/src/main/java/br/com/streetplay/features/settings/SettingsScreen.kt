package br.com.streetplay.features.settings

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.common.openSystemScreen
import br.com.streetplay.core.design.*
import br.com.streetplay.core.storage.UserPrefs
import br.com.streetplay.domain.AppMode
import java.util.Locale

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    onMode: (AppMode) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onManageFavorites: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lite = LocalLite.current
    fun system(action: String) {
        if (!context.openSystemScreen(action)) Toast.makeText(context, "Tela indisponível neste aparelho.", Toast.LENGTH_SHORT).show()
    }
    val other = if (prefs.mode == AppMode.STREET) AppMode.LITE else AppMode.STREET

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = if (lite) 56.dp else SPSpacing.xl, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.sm),
    ) {
        item { SPSectionTitle(if (lite) "Ajustes" else "Configurações") }
        item {
            SPSettingsItem(
                "Modo", { onMode(other) },
                subtitle = if (lite) null else "Street Play (preto e laranja) ou Lite (preto e branco, minimalista)",
                value = prefs.mode.label,
            )
        }
        item { SPSettingsItem("Animações", { onAnimations(!prefs.animations) }, value = if (prefs.animations) "Ligadas" else "Desligadas") }
        item { SPSettingsItem("Gerenciar favoritos", onManageFavorites) }
        item { SPSettingsItem("Aparelho", {}, value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}") }
        item { SPSettingsItem("Versão do Android", {}, value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})") }
        item { SPSettingsItem("Resolução", {}, value = resolution(context)) }
        item { SPSettingsItem("Armazenamento disponível", {}, value = freeStorage()) }
        item { SPSettingsItem("Definir como launcher padrão", { system(Settings.ACTION_HOME_SETTINGS) }, subtitle = if (lite) null else "Depende do fabricante e da versão do Android") }
        item { SPSettingsItem("Configurações do Android", { system(Settings.ACTION_SETTINGS) }) }
        item { SPSettingsItem("Wi-Fi", { system(Settings.ACTION_WIFI_SETTINGS) }) }
        item { SPSettingsItem("Bluetooth", { system(Settings.ACTION_BLUETOOTH_SETTINGS) }) }
        item { SPSettingsItem("Sobre", onAbout) }
    }
}

private fun resolution(c: Context): String =
    c.resources.displayMetrics.let { "${it.widthPixels}×${it.heightPixels} · ${it.densityDpi} dpi" }

private fun freeStorage(): String = runCatching {
    val bytes = StatFs(Environment.getDataDirectory().path).availableBytes
    String.format(Locale("pt", "BR"), "%.1f GB livres", bytes / 1_000_000_000.0)
}.getOrDefault("Indisponível")
