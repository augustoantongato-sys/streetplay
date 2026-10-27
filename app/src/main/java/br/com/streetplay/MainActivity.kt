package br.com.streetplay

import android.os.Bundle
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import br.com.streetplay.core.common.LocalBannerLoader
import br.com.streetplay.core.common.LocalIconLoader
import br.com.streetplay.core.design.SPTheme
import br.com.streetplay.core.design.LocalRemoteImage
import br.com.streetplay.features.home.DiscoverViewModel
import br.com.streetplay.features.home.LauncherViewModel
import br.com.streetplay.features.live.LiveViewModel
import br.com.streetplay.features.settings.AppViewModelFactory
import br.com.streetplay.features.settings.SettingsViewModel
import br.com.streetplay.navigation.AppShell

class MainActivity : ComponentActivity() {
    private val factory by lazy { AppViewModelFactory((application as StreetPlayApp).container) }
    private val launcherVm: LauncherViewModel by viewModels { factory }
    private val settingsVm: SettingsViewModel by viewModels { factory }
    private val discoverVm: DiscoverViewModel by viewModels { factory }
    private val liveVm: LiveViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            val prefs by settingsVm.prefs.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                launcherVm.messages.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
            }
            // A escala de referência é aplicada apenas na TV; no celular usamos a densidade nativa.
            val tvDevice = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            val dm = resources.displayMetrics
            val tvDensity = Density(density = maxOf(dm.widthPixels, dm.heightPixels) / 1280f, fontScale = 1f)
            val content: @androidx.compose.runtime.Composable () -> Unit = {
                SPTheme(prefs.mode, prefs.accent, prefs.animations) {
                    CompositionLocalProvider(
                        LocalIconLoader provides launcherVm::loadIcon,
                        LocalBannerLoader provides launcherVm::loadBanner,
                        LocalRemoteImage provides (application as StreetPlayApp).container.imageLoader::load,
                    ) { AppShell(launcherVm, settingsVm, discoverVm, liveVm) }
                }
            }
            if (tvDevice) CompositionLocalProvider(LocalDensity provides tvDensity) { content() }
            else content()
        }
    }

    override fun onResume() {
        super.onResume()
        discoverVm.refreshIfEmpty()
        launcherVm.refresh()   // apps podem ter sido instalados/removidos
    }
}
