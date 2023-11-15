package no.nav.etterlatte.token

import no.nav.security.token.support.core.jwt.JwtTokenClaims

sealed class BrukerTokenInfo {
    abstract fun ident(): String

    abstract fun erSammePerson(ident: String?): Boolean

    abstract fun getClaims(): JwtTokenClaims?

    abstract val roller: List<String>

    abstract fun accessToken(): String

    abstract fun kanEndreOppgaverFor(ident: String?): Boolean

    companion object {
        private fun erSystembruker(
            oid: String?,
            sub: String?,
        ) = (oid == sub) && (oid != null)

        fun of(
            accessToken: String,
            saksbehandler: String?,
            oid: String?,
            sub: String?,
            claims: JwtTokenClaims?,
        ): BrukerTokenInfo {
            return if (erSystembruker(oid = oid, sub = sub)) {
                Systembruker(oid!!, sub!!, saksbehandler, claims)
            } else if (saksbehandler != null) {
                Saksbehandler(accessToken, saksbehandler, claims)
            } else {
                throw Exception(
                    "Er ikke systembruker, og Navident er null i token, sannsynligvis manglende claim NAVident",
                )
            }
        }
    }
}

data class Systembruker(
    val oid: String,
    val sub: String,
    val navn: String? = null,
    val jwtTokenClaims: JwtTokenClaims? = null,
) : BrukerTokenInfo() {
    constructor(oid: String, sub: String) : this(oid, sub, null)

    override fun ident() = navn ?: Fagsaksystem.EY.navn

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")

    override fun getClaims() = jwtTokenClaims

    override val roller: List<String>
        get() = getClaims()?.getAsList("roles") ?: emptyList()

    override fun erSammePerson(ident: String?) = false

    override fun kanEndreOppgaverFor(ident: String?) = true
}

data class Saksbehandler(
    val accessToken: String,
    val ident: String,
    val jwtTokenClaims: JwtTokenClaims?,
) : BrukerTokenInfo() {
    override fun ident() = ident

    override fun accessToken() = accessToken

    override fun kanEndreOppgaverFor(ident: String?) = erSammePerson(ident)

    override fun erSammePerson(ident: String?) = ident == this.ident

    override fun getClaims() = jwtTokenClaims

    override val roller: List<String>
        get() = getClaims()?.getAsList("groups") ?: emptyList()
}

enum class Claims {
    NAVident,

    @Suppress("ktlint:standard:enum-entry-name-case")
    oid,

    @Suppress("ktlint:standard:enum-entry-name-case")
    sub,
}
