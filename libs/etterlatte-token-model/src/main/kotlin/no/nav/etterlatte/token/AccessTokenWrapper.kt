package no.nav.etterlatte.token

data class AccessTokenWrapper(
    val accessToken: String,
    val saksbehandler: Saksbehandler?,
    val oid: String?,
    val sub: String?
) {
    val saksbehandlerIdentEllerSystemnavn: String =
        if (erMaskinTilMaskin()) {
            Fagsaksystem.EY.name
        } else {
            saksbehandler!!.ident
        }

    fun erMaskinTilMaskin() = oid == sub
    fun saksbehandlerEnhet(saksbehandlere: Map<String, String>): String {
        if (erMaskinTilMaskin()) {
            return Fagsaksystem.EY.name
        }

        return saksbehandlere[saksbehandler!!.ident]
            ?: throw SaksbehandlerManglerEnhetException("Saksbehandler $saksbehandler mangler enhet fra secret")
    }

    init {
        if (!(erMaskinTilMaskin() || saksbehandler != null)) {
            throw Exception(
                "Er ikke maskin-til-maskin, og Navident er null i token, sannsynligvis manglende claim NAVident"
            )
        }
    }
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}

data class Saksbehandler(val ident: String)