package br.com.streetplay.navigation

import br.com.streetplay.core.design.SPSidebarItem

enum class Destination(val glyph: String, val label: String) {
    HOME("1", "Início"),
    APPS("2", "Aplicativos"),
    ENTERTAINMENT("3", "Filmes e séries"),
    FAVORITES("4", "Favoritos"),
    STORE("5", "Loja"),
    SETTINGS("6", "Configurações"),
    ABOUT("7", "Sobre");

    fun toSidebarItem() = SPSidebarItem(name, glyph, label)
}
