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
import br.com.streetplay.core.common.openSystemScreen
import br.com.streetplay.core.design.*
import br.com.streetplay.core.storage.UserPrefs
import java.util.Locale

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    onWallpaper: (Int) -> Unit,
    onAccent: (Int) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onManageFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    fun system(action: String) {
        if (!context.openSystemScreen(action)) Toast.makeText(context, "Tela indisponível neste aparelho.", Toast.LENGTH_SHORT).show()
    }
    val wallpaper = prefs.wallpaper.coerceIn(SPWallpapers.indices)
    val accent = prefs.accent.coerceIn(SPAccents.indices)

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = SPSpacing.xl, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.sm),
    ) {
        item { SPSectionTitle("Configurações") }
        item { SPText("Aparência", SPType.Label, color = SPColors.TextSecondary) }
        item { SPSettingsItem("Papel de parede", { onWallpaper((wallpaper + 1) % SPWallpapers.size) }, "Pressione OK para trocar", SPWallpapers[wallpaper].name) }
        item { SPSettingsItem("Cor de destaque", { onAccent((accent + 1) % SPAccents.size) }, "Tons oficiais da marca", SPAccents[accent].first) }
        item { SPSettingsItem("Animações", { onAnimations(!prefs.animations) }, "Desative em aparelhos mais lentos", if (prefs.animations) "Ligadas" else "Desligadas") }

        item { SPText("Aplicativos", SPType.Label, color = SPColors.TextSecondary) }
        item { SPSettingsItem("Gerenciar favoritos", onManageFavorites, "Reordenar e remover") }

        item { SPText("Sistema", SPType.Label, color = SPColors.TextSecondary) }
        item { SPSettingsItem("Aparelho", {}, value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}") }
        item { SPSettingsItem("Versão do Android", {}, value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})") }
        item { SPSettingsItem("Resolução", {}, value = resolution(context)) }
        item { SPSettingsItem("Armazenamento disponível", {}, value = freeStorage()) }
        item { SPSettingsItem("Definir Street Play como launcher", { system(Settings.ACTION_HOME_SETTINGS) }, "Depende do fabricante e da versão do Android") }
        item { SPSettingsItem("Configurações do Android", { system(Settings.ACTION_SETTINGS) }) }
        item { SPSettingsItem("Wi-Fi", { system(Settings.ACTION_WIFI_SETTINGS) }) }
        item { SPSettingsItem("Bluetooth", { system(Settings.ACTION_BLUETOOTH_SETTINGS) }) }
    }
}

private fun resolution(c: Context): String =
    c.resources.displayMetrics.let { "${it.widthPixels}×${it.heightPixels} · ${it.densityDpi} dpi" }

private fun freeStorage(): String = runCatching {
    val bytes = StatFs(Environment.getDataDirectory().path).availableBytes
    String.format(Locale("pt", "BR"), "%.1f GB livres", bytes / 1_000_000_000.0)
}.getOrDefault("Indisponível")
