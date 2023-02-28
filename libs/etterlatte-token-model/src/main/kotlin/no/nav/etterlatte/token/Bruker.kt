package no.nav.etterlatte.token

sealed class Bruker(
    open val accessToken: String,
    val saksbehandler: String?
) {
    val saksbehandlerIdentEllerSystemnavn: String =
        if (erMaskinTilMaskin()) {
            Fagsaksystem.EY.name
        } else {
            saksbehandler!!
        }

    abstract fun erMaskinTilMaskin(): Boolean
    fun saksbehandlerEnhet(saksbehandlere: Map<String, String>): String {
        if (erMaskinTilMaskin()) {
            return Fagsaksystem.EY.name
        }

        return saksbehandlere[saksbehandler!!]
            ?: throw SaksbehandlerManglerEnhetException("Saksbehandler $saksbehandler mangler enhet fra secret")
    }

    companion object {
        private fun erMaskinTilMaskin(oid: String?, sub: String?) = (oid == sub) && (oid != null)
        fun of(accessToken: String, saksbehandler: String?, oid: String? = null, sub: String? = null): Bruker {
            return if (erMaskinTilMaskin(oid = oid, sub = sub)) {
                System(accessToken, oid!!, sub!!)
            } else if (saksbehandler != null) {
                Saksbehandler(accessToken, saksbehandler)
            } else {
                throw Exception(
                    "Er ikke maskin-til-maskin, og Navident er null i token, sannsynligvis manglende claim NAVident"
                )
            }
        }
    }
}

data class System(override val accessToken: String, val oid: String, val sub: String) : Bruker(accessToken, null) {
    override fun erMaskinTilMaskin() = true
}

data class Saksbehandler(override val accessToken: String, val ident: String) : Bruker(accessToken, ident) {
    override fun erMaskinTilMaskin() = false
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}