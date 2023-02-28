package no.nav.etterlatte.token

data class Bruker(
    val accessToken: String,
    val saksbehandler: String?,
    val oid: String?,
    val sub: String?
) {
    val saksbehandlerIdentEllerSystemnavn: String =
        if (erMaskinTilMaskin()) {
            Fagsaksystem.EY.name
        } else {
            saksbehandler!!
        }

    fun erMaskinTilMaskin() = (oid == sub) && (oid != null)
    fun saksbehandlerEnhet(saksbehandlere: Map<String, String>): String {
        if (erMaskinTilMaskin()) {
            return Fagsaksystem.EY.name
        }

        return saksbehandlere[saksbehandler!!]
            ?: throw SaksbehandlerManglerEnhetException("Saksbehandler $saksbehandler mangler enhet fra secret")
    }

    init {
        if (!(erMaskinTilMaskin() || saksbehandler != null)) {
            throw Exception(
                "Er ikke maskin-til-maskin, og Navident er null i token, sannsynligvis manglende claim NAVident"
            )
        }
    }

    companion object {
        private fun erMaskinTilMaskin(oid: String?, sub: String?) = (oid == sub) && (oid != null)
        fun of(accessToken: String, saksbehandler: String?, oid: String? = null, sub: String? = null): Bruker {
            return if (erMaskinTilMaskin(oid = oid, sub = sub)) {
                Bruker(accessToken, null, oid, sub)
            } else if (saksbehandler != null) {
                Bruker(accessToken, saksbehandler, null, null)
            } else {
                throw Exception(
                    "Er ikke maskin-til-maskin, og Navident er null i token, sannsynligvis manglende claim NAVident"
                )
            }
        }
    }
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}