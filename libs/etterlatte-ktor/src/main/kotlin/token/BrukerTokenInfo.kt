package no.nav.etterlatte.libs.ktor.token

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.token.support.core.jwt.JwtTokenClaims

const val APP = "app"

sealed class BrukerTokenInfo {
    abstract fun ident(): String

    abstract fun erSammePerson(ident: String?): Boolean

    abstract fun getClaims(): JwtTokenClaims?

    abstract fun accessToken(): String

    abstract fun kanEndreOppgaverFor(ident: String?): Boolean

    companion object {
        private fun erSystembruker(idtyp: String?) = idtyp != null && idtyp == APP

        @PublishedApi
        internal fun of(
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

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")

    override fun getClaims() = jwtTokenClaims

    val roller: List<String>
        get() = getClaims()?.getAsList(Claims.roles.name) ?: emptyList()

    override fun erSammePerson(ident: String?) = false

    override fun kanEndreOppgaverFor(ident: String?) = true
}

@ConsistentCopyVisibility
data class VanligSystembruker internal constructor(
    override val ident: String,
    override val jwtTokenClaims: JwtTokenClaims? = null,
) : Systembruker(ident, jwtTokenClaims)

@ConsistentCopyVisibility
data class HardkodaSystembruker private constructor(
    val omraade: Systembrukere,
) : Systembruker(ident = omraade.appName, jwtTokenClaims = tokenMedClaims(mapOf(Claims.idtyp to APP))) {
    companion object {
        /* Obs: Bruk bare disse fra kontekster hvor vi ikke har et BrukerTokenInfo-objekt allerede.
        Send alltid med brukeren fra requesten der du kan. Disse er for rivers, automatiske jobber
        og testdata-appene, hvor systemet på eget initiativ starter kjøring, og vi dermed ikke har en
        eksisterende saksbehandler eller systembruker å hente tokenet fra
         */
        val river = HardkodaSystembruker(Systembrukere.RIVER)
        val doedshendelse = HardkodaSystembruker(Systembrukere.DOEDSHENDELSE)
        val aktivitetsplikt = HardkodaSystembruker(Systembrukere.AKTIVITETSPLIKT)
        val testdata = HardkodaSystembruker(Systembrukere.TESTDATA)
        val oppgave = HardkodaSystembruker(Systembrukere.OPPGAVE)
        val opprettGrunnlag = HardkodaSystembruker(Systembrukere.OPPRETT_GRUNNLAG) // skal bort på sikt
        val ryddeBeregning = HardkodaSystembruker(Systembrukere.BEREGNING)
        val omregning = HardkodaSystembruker(Systembrukere.OMREGNING)
        val uttrekk = HardkodaSystembruker(Systembrukere.UTTREKK)
        val statistikk = HardkodaSystembruker(Systembrukere.STATISTIKK)
        val tilgang = HardkodaSystembruker(Systembrukere.TILGANG)
        val etteroppgjoer = HardkodaSystembruker(Systembrukere.ETTEROPPGJOER)
        val institusjonsopphold = HardkodaSystembruker(Systembrukere.ETTEROPPGJOER)
    }

    enum class Systembrukere(
        val appName: String,
    ) {
        TILGANG("tilgang"),
        OPPRETT_GRUNNLAG("opprettgrunnlag"),
        RIVER("river"),
        DOEDSHENDELSE("doedshendelse"),
        AKTIVITETSPLIKT("aktivitetsplikt"),
        TESTDATA("testdata"),
        OPPGAVE("oppgave"),
        BEREGNING("beregning"),
        OMREGNING("omregning"),
        STATISTIKK("statistikk"),
        UTTREKK("uttrekk"),
        ETTEROPPGJOER("etteroppgjoer"),
        INSTITUSJONSOPPHOLD("institusjonsopphold"),
    }
}

@ConsistentCopyVisibility
data class Saksbehandler internal constructor(
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

fun tokenMedClaims(claims: Map<Claims, Any?>) =
    claims.entries
        .fold(JWTClaimsSet.Builder()) { acc, next -> acc.claim(next.key.name, next.value) }
        .build()
        .let { JwtTokenClaims(it) }
