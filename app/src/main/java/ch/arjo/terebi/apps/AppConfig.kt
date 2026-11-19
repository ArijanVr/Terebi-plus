package ch.arjo.terebi.apps

/**
 * Was terebi+ über Apps speichert. Ausblenden betrifft nur terebi+, nie das System.
 * [known] unterscheidet «neu installiert» (erscheint am Ende, sichtbar) von «bewusst ausgeblendet».
 */
data class AppConfig(
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val known: Set<String> = emptySet(),
) {
    /** Abgleich mit den installierten, startbaren Paketen: Neue anhängen, Deinstallierte entfernen. */
    fun sync(installed: List<String>): AppConfig {
        val inst = installed.toSet()
        val kept = order.filter { it in inst }
        val added = installed.filter { it !in kept && it !in known }
        val readded = installed.filter { it !in kept && it in known } // known, aber aus order gefallen
        return AppConfig(
            order = kept + added + readded,
            hidden = hidden intersect inst,
            favorites = favorites intersect inst,
            known = inst,
        )
    }

    /** Reihenfolge der Startseite: Favoriten zuerst, sonst Benutzerreihenfolge. */
    fun home(): List<String> {
        val vis = order.filter { it !in hidden }
        return vis.filter { it in favorites } + vis.filter { it !in favorites }
    }

    fun toggleHidden(pkg: String) = copy(hidden = if (pkg in hidden) hidden - pkg else hidden + pkg)
    fun toggleFavorite(pkg: String) = copy(favorites = if (pkg in favorites) favorites - pkg else favorites + pkg)

    /** Verschiebt [pkg] in der Liste um [delta]. */
    fun moveInOrder(pkg: String, delta: Int): AppConfig {
        val i = order.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j !in order.indices) return this
        return copy(order = order.toMutableList().apply { this[i] = this[j].also { this[j] = this[i] } })
    }

    /** Tauscht [pkg] auf der Startseite mit dem Nachbarn in [homeDelta]; Favoriten bleiben vorne. */
    fun moveOnHome(pkg: String, homeDelta: Int): AppConfig {
        val home = home()
        val i = home.indexOf(pkg)
        val j = i + homeDelta
        if (i < 0 || j !in home.indices) return this
        val other = home[j]
        if ((pkg in favorites) != (other in favorites)) return this
        val a = order.indexOf(pkg)
        val b = order.indexOf(other)
        return copy(order = order.toMutableList().apply { this[a] = other; this[b] = pkg })
    }
}
