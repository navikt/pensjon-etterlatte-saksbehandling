package no.nav.etterlatte.libs.ktor.token

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
        ): BrukerTokenInfo =
            if (erSystembruker(oid = oid, sub = sub)) {
                Systembruker(oid!!, sub!!, ident = claims?.getClaimAsString(Claims.azp_name) ?: sub, claims)
            } else if (saksbehandler != null) {
                Saksbehandler(accessToken, ident = saksbehandler, claims)
            } else {
                throw Exception(
                    "Er ikke systembruker, og Navident er null i token, sannsynligvis manglende claim NAVident",
                )
            }
    }
}

data class Systembruker(
    val oid: String,
    val sub: String,
    val ident: String? = null,
    val jwtTokenClaims: JwtTokenClaims? = null,
) : BrukerTokenInfo() {
    private constructor(omraade: Systembrukere) : this(oid = omraade.oid, sub = omraade.oid)

    override fun ident() = ident ?: Fagsaksystem.EY.navn

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")

    override fun getClaims() = jwtTokenClaims

    override val roller: List<String>
        get() = getClaims()?.getAsList(Claims.roles.name) ?: emptyList()

    override fun erSammePerson(ident: String?) = false

    override fun kanEndreOppgaverFor(ident: String?) = true

    companion object {
        val migrering = Systembruker(Systembrukere.MIGRERING)
        val brev = Systembruker(Systembrukere.BREV)
        val doedshendelse = Systembruker(Systembrukere.DOEDSHENDELSE)
        val regulering = Systembruker(Systembrukere.REGULERING)
        val tekniskRetting = Systembruker(Systembrukere.TEKNISK_RETTING)
        val testdata = Systembruker(Systembrukere.TESTDATA)
        val automatiskJobb = Systembruker(Systembrukere.AUTOMATISK_JOBB)
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
    MIGRERING("migrering"),
    DOEDSHENDELSE("doedshendelse"),
    REGULERING("regulering"),
    TEKNISK_RETTING("teknisk_retting"),
    TESTDATA("testdata"),
    AUTOMATISK_JOBB("automatisk_jobb"),
}
