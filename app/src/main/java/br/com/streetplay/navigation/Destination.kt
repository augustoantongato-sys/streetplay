package br.com.streetplay.navigation

import br.com.streetplay.core.design.SPIconKind
import br.com.streetplay.core.design.SPSidebarItem

/** inMenu = aparece no menu lateral; as demais são telas internas. */
enum class Destination(val icon: SPIconKind, val label: String, val inMenu: Boolean = true) {
    HOME(SPIconKind.HOME, "Início"),
    APPS(SPIconKind.APPS, "Aplicativos"),
    ENTERTAINMENT(SPIconKind.STREAMING, "Apps de streaming"),
    SERVICES(SPIconKind.STREAMING, "Serviços"),
    LIBRARY(SPIconKind.VIDEO, "Filmes e séries"),
    LIVE(SPIconKind.TV, "Canais ao vivo"),
    FAVORITES(SPIconKind.FAVORITES, "Favoritos", inMenu = false),
    STORE(SPIconKind.STORE, "Loja"),
    SETTINGS(SPIconKind.SETTINGS, "Configurações"),
    ABOUT(SPIconKind.INFO, "Sobre"),
    ADD_APPS(SPIconKind.PLUS, "Adicionar apps", inMenu = false);

    fun toSidebarItem() = SPSidebarItem(name, icon, label)
}
