package br.com.streetplay.features.settings

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import br.com.streetplay.core.common.openSystemScreen
import br.com.streetplay.core.design.*
import br.com.streetplay.core.storage.UserPrefs
import br.com.streetplay.domain.AccentTheme
import br.com.streetplay.domain.AppMode
import java.util.Locale

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    onMode: (AppMode) -> Unit,
    onAccent: (AccentTheme) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onManageFavorites: () -> Unit,
    onAbout: () -> Unit,
    iptvConnected: Boolean,
    iptvMessage: String,
    onSaveIptv: (String, String, String) -> Unit,
    onDisconnectIptv: () -> Unit,
    modifier: Modifier = Modifier,
    forceIptvLogin: Boolean = false,
) {
    val context = LocalContext.current
    val lite = LocalLite.current
    fun system(action: String) {
        if (!context.openSystemScreen(action)) Toast.makeText(context, "Tela indisponível neste aparelho.", Toast.LENGTH_SHORT).show()
    }
    val other = if (prefs.mode == AppMode.STREET) AppMode.LITE else AppMode.STREET
    var pickColor by remember { mutableStateOf(false) }
    var showIptv by remember { mutableStateOf(forceIptvLogin) }

    if (forceIptvLogin) {
        FullScreenIptvLogin(message = iptvMessage, onSave = onSaveIptv)
        return
    }

    if (pickColor) ColorPicker(prefs.accent, onAccent) { pickColor = false }

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
        if (!lite) item {
            SPSettingsItem("Cor principal", { pickColor = true }, subtitle = "Botões, destaques, seleções e bordas", value = prefs.accent.label)
        }
        item { SPSettingsItem("Animações", { onAnimations(!prefs.animations) }, value = if (prefs.animations) "Ligadas" else "Desligadas") }
        item { SPSettingsItem("Conta IPTV (Xtream Codes)", { showIptv = true }, subtitle = "Use os dados fornecidos pelo seu serviço IPTV", value = if (iptvConnected) "Conectada" else "Não configurada") }
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
    if (showIptv) IptvAccountDialog(
        connected = iptvConnected, message = iptvMessage,
        onSave = onSaveIptv, onDisconnect = onDisconnectIptv,
        onDismiss = { if (!forceIptvLogin) showIptv = false },
    )
}

@Composable
private fun IptvAccountDialog(
    connected: Boolean,
    message: String,
    onSave: (String, String, String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    SPDialog("Conta IPTV", onDismiss) {
        SPText("Informe o servidor, usuário e senha fornecidos pelo seu próprio provedor. Os dados ficam cifrados neste aparelho. Prefira HTTPS: em servidores HTTP, os dados trafegam sem criptografia pela rede.", SPType.Body, color = SPColors.TextSecondary)
        IptvInput(server, { server = it }, "Servidor (ex.: https://servidor.com)")
        IptvInput(username, { username = it }, "Usuário")
        IptvInput(password, { password = it }, "Senha", secret = true)
        if (message.isNotBlank()) SPText(message, SPType.Caption, color = if (connected) Color(0xFF55CC88) else SPColors.TextSecondary)
        SPButton("Testar e conectar", onClick = { onSave(server, username, password) })
        if (connected) SPButton("Desconectar conta", onDisconnect)
        SPButton("Fechar", onDismiss)
    }
}

@Composable
private fun IptvInput(value: String, onValue: (String) -> Unit, hint: String, secret: Boolean = false) {
    BasicTextField(
        value = value, onValueChange = onValue, singleLine = true,
        textStyle = TextStyle(color = Color.White),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().background(Color(0xFF202020), RoundedCornerShape(9.dp)).padding(12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) SPText(hint, SPType.Caption, color = SPColors.TextSecondary)
                inner()
            }
        },
    )
}

@Composable
private fun ColorPicker(current: AccentTheme, onPick: (AccentTheme) -> Unit, onDismiss: () -> Unit) {
    SPDialog("Cor principal", onDismiss) {
        AccentTheme.entries.forEach { t ->
            SPFocusIndicator(onClick = { onPick(t); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(Color(t.argb)))
                    Spacer(Modifier.width(14.dp))
                    SPText(t.label, SPType.Body, Modifier.weight(1f))
                    if (t == current) SPText("Atual", SPType.Caption)
                }
            }
        }
        SPButton("Fechar", onDismiss)
    }
}

private fun resolution(c: Context): String =
    c.resources.displayMetrics.let { "${it.widthPixels}×${it.heightPixels} · ${it.densityDpi} dpi" }

private fun freeStorage(): String = runCatching {
    val bytes = StatFs(Environment.getDataDirectory().path).availableBytes
    String.format(Locale("pt", "BR"), "%.1f GB livres", bytes / 1_000_000_000.0)
}.getOrDefault("Indisponível")

@Composable
private fun FullScreenIptvLogin(message: String, onSave: (String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    val orange = Color(0xFFFF7417)
    Box(Modifier.fillMaxSize().background(Color(0xFF050505)).padding(36.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().widthIn(max = 620.dp).background(Color(0xFF11100F), RoundedCornerShape(30.dp)).padding(horizontal = 42.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SPText("▶", TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Black, color = orange))
            SPText("IPTV", TextStyle(fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White))
            SPText("P L A Y", TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = orange))
            SPText("Sua TV, do seu jeito.", SPType.Body, color = Color(0xFFB9B3AE))
            FullScreenLoginField(username, { username = it }, "Usuário")
            FullScreenLoginField(password, { password = it }, "Senha", secret = true)
            FullScreenLoginField(server, { server = it }, "Servidor")
            if (message.isNotBlank()) SPText(message, SPType.Caption, color = Color(0xFFFFB28A))
            SPButton("ENTRAR", onClick = { onSave(server.trim(), username.trim(), password) })
        }
    }
}

@Composable
private fun FullScreenLoginField(value: String, onValue: (String) -> Unit, hint: String, secret: Boolean = false) {
    BasicTextField(value = value, onValueChange = onValue, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 20.sp), visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, modifier = Modifier.fillMaxWidth().background(Color(0xFF24201D), RoundedCornerShape(18.dp)).padding(horizontal = 22.dp, vertical = 19.dp), decorationBox = { inner ->
        Box { if (value.isEmpty()) SPText(hint, SPType.Body, color = Color(0xFFAAA19A)); inner() }
    })
}
