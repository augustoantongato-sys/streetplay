package br.com.streetplay

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.streetplay.core.common.LocalIconLoader
import br.com.streetplay.core.design.SPTheme
import br.com.streetplay.features.home.LauncherViewModel
import br.com.streetplay.features.settings.AppViewModelFactory
import br.com.streetplay.features.settings.SettingsViewModel
import br.com.streetplay.navigation.AppShell

class MainActivity : ComponentActivity() {
    private val factory by lazy { AppViewModelFactory((application as StreetPlayApp).container) }
    private val launcherVm: LauncherViewModel by viewModels { factory }
    private val settingsVm: SettingsViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by settingsVm.prefs.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                launcherVm.messages.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
            }
            // Tela de referência de 1280 dp de largura: o layout fica igual em 720p, 1080p ou 4K.
            val dm = resources.displayMetrics
            val density = Density(density = maxOf(dm.widthPixels, dm.heightPixels) / 1280f, fontScale = 1f)
            CompositionLocalProvider(LocalDensity provides density) {
                SPTheme(prefs.accent, prefs.animations) {
                    CompositionLocalProvider(LocalIconLoader provides launcherVm::loadIcon) {
                        AppShell(launcherVm, settingsVm)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launcherVm.refresh()   // apps podem ter sido instalados/removidos
    }
}
