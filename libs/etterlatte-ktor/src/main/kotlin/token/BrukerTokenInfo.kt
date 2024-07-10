package no.nav.etterlatte.libs.ktor.token

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
                Systembruker(oid!!, sub!!, ident = claims?.getStringClaim(Claims.azp_name.name) ?: sub, claims)
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
        get() = getClaims()?.getAsList("roles") ?: emptyList()

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
        get() = getClaims()?.getAsList("groups") ?: emptyList()
}

enum class Claims {
    NAVident,

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
