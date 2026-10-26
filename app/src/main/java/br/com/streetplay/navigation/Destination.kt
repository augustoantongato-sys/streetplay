package br.com.streetplay.navigation

import br.com.streetplay.core.design.SPIconKind
import br.com.streetplay.core.design.SPSidebarItem

/** inMenu = aparece no menu lateral; as demais são telas internas. */
enum class Destination(val icon: SPIconKind, val label: String, val inMenu: Boolean = true) {
    HOME(SPIconKind.HOME, "Início"),
    APPS(SPIconKind.APPS, "Aplicativos", inMenu = false),
    ENTERTAINMENT(SPIconKind.STREAMING, "Apps de streaming", inMenu = false),
    SERVICES(SPIconKind.STREAMING, "Serviços", inMenu = false),
    LIBRARY(SPIconKind.STREAMING, "Filmes e séries"),
    LIVE(SPIconKind.TV, "Canais ao vivo"),
    FAVORITES(SPIconKind.FAVORITES, "Favoritos", inMenu = false),
    STORE(SPIconKind.STORE, "Loja", inMenu = false),
    SETTINGS(SPIconKind.SETTINGS, "Configurações"),
    ABOUT(SPIconKind.INFO, "Sobre"),
    ADD_APPS(SPIconKind.PLUS, "Adicionar apps", inMenu = false);

    fun toSidebarItem() = SPSidebarItem(name, icon, label)
}
