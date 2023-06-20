package no.nav.etterlatte.token

import no.nav.security.token.support.core.jwt.JwtTokenClaims

sealed class BrukerTokenInfo {
    abstract fun ident(): String

    abstract fun accessToken(): String
    companion object {
        private fun erSystembruker(oid: String?, sub: String?) = (oid == sub) && (oid != null)
        fun of(
            accessToken: String,
            saksbehandler: String?,
            oid: String?,
            sub: String?,
            claims: JwtTokenClaims?
        ): BrukerTokenInfo {
            return if (erSystembruker(oid = oid, sub = sub)) {
                Systembruker(oid!!, sub!!)
            } else if (saksbehandler != null) {
                Saksbehandler(accessToken, saksbehandler, claims)
            } else {
                throw Exception(
                    "Er ikke systembruker, og Navident er null i token, sannsynligvis manglende claim NAVident"
                )
            }
        }
    }
}

data class Systembruker(val oid: String, val sub: String) : BrukerTokenInfo() {
    override fun ident() = Fagsaksystem.EY.navn

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")
}

data class Saksbehandler(
    val accessToken: String,
    val ident: String,
    val jwtTokenClaims: JwtTokenClaims?
) : BrukerTokenInfo() {
    override fun ident() = ident

    override fun accessToken() = accessToken

    fun getClaims() = jwtTokenClaims
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}