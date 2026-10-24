package br.com.streetplay.core.common

/** Move o item [index] em [delta] posições. Retorna a mesma lista (===) se o movimento for inválido. */
fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices || delta == 0) return this
    return toMutableList().also { it.add(target, it.removeAt(index)) }
}
