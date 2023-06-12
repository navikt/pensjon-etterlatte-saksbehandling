package no.nav.etterlatte.token

import no.nav.etterlatte.config.AzureGroup
import no.nav.security.token.support.core.jwt.JwtTokenClaims

sealed class Bruker {
    abstract fun ident(): String

    abstract fun accessToken(): String
    abstract fun kanAttestereFor(ansvarligSaksbehandler: String): Boolean
    abstract fun harRolle(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>, rolle: AzureGroup): Boolean

    companion object {
        private fun erSystembruker(oid: String?, sub: String?) = (oid == sub) && (oid != null)
        fun of(
            accessToken: String,
            saksbehandler: String?,
            oid: String?,
            sub: String?,
            claims: JwtTokenClaims?
        ): Bruker {
            return if (erSystembruker(oid = oid, sub = sub)) {
                SystemBruker(oid!!, sub!!)
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

data class SystemBruker(val oid: String, val sub: String) : Bruker() {
    override fun ident() = Fagsaksystem.EY.navn

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")
    override fun kanAttestereFor(ansvarligSaksbehandler: String) = true
    override fun harRolle(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>, rolle: AzureGroup) = when (rolle) {
        AzureGroup.STRENGT_FORTROLIG -> false
        AzureGroup.FORTROLIG -> false
        AzureGroup.EGEN_ANSATT -> false
        else -> true
    }
}

data class Saksbehandler(val accessToken: String, val ident: String, val jwtTokenClaims: JwtTokenClaims?) : Bruker() {
    override fun ident() = ident

    override fun accessToken() = accessToken
    override fun kanAttestereFor(ansvarligSaksbehandler: String) = ansvarligSaksbehandler != this.ident()
    override fun harRolle(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>, rolle: AzureGroup) =
        jwtTokenClaims?.containsClaim("groups", saksbehandlerGroupIdsByKey[rolle]) ?: false
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}