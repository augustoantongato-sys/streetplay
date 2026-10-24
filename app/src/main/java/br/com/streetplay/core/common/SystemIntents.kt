package br.com.streetplay.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Abre uma tela nativa de configurações do Android. Retorna false se o aparelho não tiver a tela. */
fun Context.openSystemScreen(action: String): Boolean = try {
    startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
} catch (e: ActivityNotFoundException) { false } catch (e: SecurityException) { false }

fun Context.networkLabel(): String = runCatching {
    val cm = getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
    when {
        caps == null -> "Sem rede"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Conectado"
    }
}.getOrDefault("Rede indisponível")
