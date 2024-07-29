package no.nav.etterlatte.libs.ktor.token

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.token.support.core.jwt.JwtTokenClaims

sealed class BrukerTokenInfo {
    abstract fun ident(): String

    abstract fun erSammePerson(ident: String?): Boolean

    abstract fun getClaims(): JwtTokenClaims?

    abstract fun accessToken(): String

    abstract fun kanEndreOppgaverFor(ident: String?): Boolean

    companion object {
        private fun erSystembruker(idtyp: String?) = idtyp != null && idtyp == "app"

        fun of(
            accessToken: String,
            saksbehandler: String?,
            claims: JwtTokenClaims?,
            idtyp: String?,
        ): BrukerTokenInfo =
            if (erSystembruker(idtyp = idtyp)) {
                VanligSystembruker(ident = claims?.getClaimAsString(Claims.azp_name)!!, claims)
            } else if (saksbehandler != null) {
                Saksbehandler(accessToken, ident = saksbehandler, claims)
            } else {
                throw Exception(
                    "Er ikke systembruker, og Navident er null i token, sannsynligvis manglende claim NAVident",
                )
            }
    }
}

sealed class Systembruker(
    open val ident: String,
    open val jwtTokenClaims: JwtTokenClaims? = null,
) : BrukerTokenInfo() {
    override fun ident() = ident

    fun identForBrev(): String {
        val systemBrukereInternt = Systembrukere.entries.map { it.appName }
        if (ident in systemBrukereInternt) {
            return Fagsaksystem.EY.navn
        } else {
            return ident
        }
    }

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")

    override fun getClaims() = jwtTokenClaims

    val roller: List<String>
        get() = getClaims()?.getAsList(Claims.roles.name) ?: emptyList()

    override fun erSammePerson(ident: String?) = false

    override fun kanEndreOppgaverFor(ident: String?) = true

    companion object {
        val river = HardkodaSystembruker(Systembrukere.RIVER)
        val doedshendelse = HardkodaSystembruker(Systembrukere.DOEDSHENDELSE)
        val testdata = HardkodaSystembruker(Systembrukere.TESTDATA)
    }
}

data class VanligSystembruker(
    override val ident: String,
    override val jwtTokenClaims: JwtTokenClaims? = null,
) : Systembruker(ident, jwtTokenClaims)

data class HardkodaSystembruker(
    val omraade: Systembrukere,
) : Systembruker(
        ident = omraade.appName,
        jwtTokenClaims =
            JwtTokenClaims(
                JWTClaimsSet.Builder().claim(Claims.idtyp.name, "app").build(),
            ),
    )

data class Saksbehandler(
    val accessToken: String,
    val ident: String,
    val jwtTokenClaims: JwtTokenClaims?,
) : BrukerTokenInfo() {
    override fun ident() = ident

    override fun accessToken() = accessToken

    override fun kanEndreOppgaverFor(ident: String?) = erSammePerson(ident) || ident == Fagsaksystem.EY.navn

    override fun erSammePerson(ident: String?) = ident == this.ident

    override fun getClaims() = jwtTokenClaims

    val groups: List<String>
        get() = getClaims()?.getAsList(Claims.groups.name) ?: emptyList()
}

enum class Claims {
   /*
    This is a special claim used to determine whether a token is a machine-to-machine (app-only) token or a on-behalf-of (user) token.
    https://docs.nais.io/auth/entra-id/reference/?h=idtyp#claims
    https://learn.microsoft.com/en-us/entra/identity-platform/optional-claims-reference#v10-and-v20-optional-claims-set
    */
    @Suppress("ktlint:standard:enum-entry-name-case")
    idtyp,

   /*
   Kun for Saksbehandlertoken!
   The internal identifier for the employees in NAV. Only applies in flows where a user is involved i.e., either the login or on-behalf-of flows.
   https://docs.nais.io/auth/entra-id/reference/?h=NAVident#claims
    */
    NAVident,

    /*
    Kun for Saksbehandlertoken!
    JSON array of group identifiers that the user is a member of.
    https://docs.nais.io/auth/entra-id/reference/?h=groups#claims
     */
    @Suppress("ktlint:standard:enum-entry-name-case")
    groups,

    /*
    Kun for systembruker!
    Roles will appear in the roles claim as an array of strings within the application's token.
    https://docs.nais.io/auth/entra-id/reference/?h=groups#custom-roles
     */
    @Suppress("ktlint:standard:enum-entry-name-case")
    roles,

    /*
    The value of this claim is the (human-readable) name of the consumer application that requested the token.
    https://docs.nais.io/auth/entra-id/reference/?h=azp_name#claims
     */
    @Suppress("ktlint:standard:enum-entry-name-case")
    azp_name,

    // systembruker applikasjonsnavn
    @Suppress("ktlint:standard:enum-entry-name-case")
    oid,

    @Suppress("ktlint:standard:enum-entry-name-case")
    sub,
}

enum class Systembrukere(
    val appName: String,
) {
    RIVER("river"),
    DOEDSHENDELSE("doedshendelse"),
    TESTDATA("testdata"),
}
