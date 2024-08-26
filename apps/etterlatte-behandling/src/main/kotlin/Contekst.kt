package no.nav.etterlatte

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.getClaimAsString
import no.nav.etterlatte.libs.ktor.token.hentTokenClaimsForIssuerName
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory
import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst,
    val sakTilgangDao: SakTilgangDao,
    val brukerTokenInfo: BrukerTokenInfo,
) {
    fun appUserAsSaksbehandler(): SaksbehandlerMedEnheterOgRoller = this.AppUser as SaksbehandlerMedEnheterOgRoller
}

interface User {
    fun name(): String
}

abstract class ExternalUser(
    val identifiedBy: TokenValidationContext,
) : User

class Self(
    private val prosess: String,
) : User {
    override fun name() = prosess
}

class SystemUser(
    identifiedBy: TokenValidationContext,
    val brukerTokenInfo: BrukerTokenInfo,
) : ExternalUser(identifiedBy) {
    override fun name(): String =
        identifiedBy
            .hentTokenClaimsForIssuerName(Issuer.AZURE)
            ?.getClaimAsString(Claims.azp_name) // format=cluster:namespace:app-name
            ?: throw IllegalArgumentException("Støtter ikke navn på systembruker")
}

class SaksbehandlerMedEnheterOgRoller(
    identifiedBy: TokenValidationContext,
    private val saksbehandlerService: SaksbehandlerService,
    val saksbehandlerMedRoller: SaksbehandlerMedRoller,
    val brukerTokenInfo: BrukerTokenInfo,
) : ExternalUser(identifiedBy) {
    val logger = LoggerFactory.getLogger(this::class.java)

    private fun saksbehandlersEnheter() =
        saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(name()).map { it.enhetsNummer }.toSet()

    override fun name(): String = identifiedBy.hentTokenClaimsForIssuerName(Issuer.AZURE)!!.getClaimAsString(Claims.NAVident)

    private fun harKjentEnhet(saksbehandlersEnheter: Set<String>) = Enheter.kjenteEnheter().intersect(saksbehandlersEnheter).isNotEmpty()

    fun kanSeOppgaveBenken() =
        saksbehandlersEnheter().any { enhetNr ->
            Enheter.entries.firstOrNull { it.enhetNr == enhetNr }?.harTilgangTilOppgavebenken ?: false
        }

    fun enheterMedSkrivetilgang() = enheterMedSaksbehandlendeEnheter(saksbehandlersEnheter())

    private fun enheterMedSaksbehandlendeEnheter(saksbehandlersEnheter: Set<String>) =
        saksbehandlersEnheter.filter { Enheter.saksbehandlendeEnheter().contains(it) }

    // TODO - EY-3441 - lesetilgang for forvaltningsutviklere
    fun enheterMedLesetilgang(saksbehandlersEnheter: Set<String>) =
        if (harKjentEnhet(saksbehandlersEnheter)) {
            enheterMedSaksbehandlendeEnheter(saksbehandlersEnheter).let { egenSkriveEnheter ->
                when (egenSkriveEnheter.size) {
                    0 -> Enheter.enheterForVanligSaksbehandlere()
                    else -> Enheter.enheterForVanligSaksbehandlere() - egenSkriveEnheter.toSet()
                }
            }
        } else {
            logger.info("Ukjent enhet på saksbehandler, får ikke lesetilgang. Enheter for saksbehandler: $saksbehandlersEnheter ")
            emptyList()
        }

    fun enheter(): List<String> {
        val saksbehandlersEnheter = saksbehandlersEnheter()
        return (enheterMedSaksbehandlendeEnheter(saksbehandlersEnheter) + enheterMedLesetilgang(saksbehandlersEnheter)).distinct()
    }
}

fun decideUser(
    principal: TokenValidationContextPrincipal,
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
    saksbehandlerService: SaksbehandlerService,
    brukerTokenInfo: BrukerTokenInfo,
): ExternalUser =
    if (principal.context.issuers.contains(Issuer.AZURE.issuerName)) {
        if (brukerTokenInfo is Systembruker) {
            SystemUser(principal.context, brukerTokenInfo)
        } else {
            SaksbehandlerMedEnheterOgRoller(
                principal.context,
                saksbehandlerService,
                SaksbehandlerMedRoller(brukerTokenInfo as Saksbehandler, saksbehandlerGroupIdsByKey),
                brukerTokenInfo,
            )
        }
    } else {
        throw IllegalStateException("no token from preapproved issuers")
    }

interface DatabaseKontekst {
    fun activeTx(): Connection

    fun harIntransaction(): Boolean

    fun <T> inTransaction(block: () -> T): T
}

fun <T> inTransaction(block: () -> T): T =
    Kontekst.get().databasecontxt.inTransaction {
        block()
    }

fun databaseContext() = Kontekst.get().databasecontxt
