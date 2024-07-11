package no.nav.etterlatte.libs.ktor.token

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.etterlatte.libs.ktor.getClaimAsString
import no.nav.security.token.support.core.jwt.JwtTokenClaims

sealed class BrukerTokenInfo {
    abstract fun ident(): String

    abstract fun erSammePerson(ident: String?): Boolean

    abstract fun getClaims(): JwtTokenClaims?

    abstract val roller: List<String>

    abstract fun accessToken(): String

    abstract fun kanEndreOppgaverFor(ident: String?): Boolean

    companion object {
        private fun erSystembruker(idtyp: String?) = idtyp != null && idtyp == "app"

        fun of(
            accessToken: String,
            saksbehandler: String?,
            oid: String?,
            sub: String?,
            claims: JwtTokenClaims?,
            idtyp: String?,
        ): BrukerTokenInfo =
            if (erSystembruker(idtyp = idtyp)) {
                VanligSystembruker(oid!!, sub!!, ident = claims?.getClaimAsString(Claims.azp_name) ?: sub, claims)
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
    open val oid: String,
    open val sub: String,
    open val ident: String? = null,
    open val jwtTokenClaims: JwtTokenClaims? = null,
) : BrukerTokenInfo() {
    override fun ident() = ident ?: Fagsaksystem.EY.navn

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")

    override fun getClaims() = jwtTokenClaims

    override val roller: List<String>
        get() = getClaims()?.getAsList(Claims.roles.name) ?: emptyList()

    override fun erSammePerson(ident: String?) = false

    override fun kanEndreOppgaverFor(ident: String?) = true

    companion object {
        val testdata = HardkodaSystembruker(Systembrukere.TESTDATA)
    }
}

data class VanligSystembruker(
    override val oid: String,
    override val sub: String,
    override val ident: String? = null,
    override val jwtTokenClaims: JwtTokenClaims? = null,
) : Systembruker(oid, sub, ident, jwtTokenClaims)

data class HardkodaSystembruker(
    val omraade: Systembrukere,
) : Systembruker(
        oid = omraade.oid,
        sub = omraade.oid,
        jwtTokenClaims =
            JwtTokenClaims(
                JWTClaimsSet.Builder().claim(Claims.idtyp.name, "app").build(),
            ),
    ) {
    companion object {
        val river = HardkodaSystembruker(Systembrukere.RIVER)
        val jobb = HardkodaSystembruker(Systembrukere.JOBB)
    }
}

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

    override val roller: List<String>
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
   The internal identifier for the employees in NAV. Only applies in flows where a user is involved i.e., either the login or on-behalf-of flows.
   https://docs.nais.io/auth/entra-id/reference/?h=NAVident#claims
    */
    NAVident,

    /*
    JSON array of group identifiers that the user is a member of.
    https://docs.nais.io/auth/entra-id/reference/?h=groups#claims
     */
    @Suppress("ktlint:standard:enum-entry-name-case")
    groups,

    /*
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
    val oid: String,
) {
    BREV("brev"),
    DOEDSHENDELSE("doedshendelse"),
    TESTDATA("testdata"),
    RIVER("river"),
    JOBB("jobb"),
}
