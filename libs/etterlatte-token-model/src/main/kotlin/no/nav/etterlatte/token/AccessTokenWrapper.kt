package no.nav.etterlatte.token

data class AccessTokenWrapper(
    val accessToken: String,
    val saksbehandler: Saksbehandler?,
    val oid: String?,
    val sub: String?
) {
    fun erMaskinTilMaskin() = oid == sub

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