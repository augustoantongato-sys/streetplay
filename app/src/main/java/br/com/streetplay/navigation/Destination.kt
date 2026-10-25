package br.com.streetplay.navigation

import br.com.streetplay.core.design.SPIconKind
import br.com.streetplay.core.design.SPSidebarItem

enum class Destination(val icon: SPIconKind, val label: String) {
    HOME(SPIconKind.HOME, "Início"),
    APPS(SPIconKind.APPS, "Aplicativos"),
    ENTERTAINMENT(SPIconKind.STREAMING, "Streaming"),
    LIVE(SPIconKind.TV, "Canais ao vivo"),
    FAVORITES(SPIconKind.FAVORITES, "Favoritos"),
    STORE(SPIconKind.STORE, "Loja"),
    SETTINGS(SPIconKind.SETTINGS, "Configurações"),
    ABOUT(SPIconKind.INFO, "Sobre");

    fun toSidebarItem() = SPSidebarItem(name, icon, label)
}
