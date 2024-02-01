package no.nav.etterlatte

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.hentTokenClaims
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.tilgangsstyring.saksbehandlereMedTilgangTilAlleEnheter
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.Systembruker
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst,
) {
    fun appUserAsSaksbehandler(): SaksbehandlerMedEnheterOgRoller {
        return this.AppUser as SaksbehandlerMedEnheterOgRoller
    }
}

interface User {
    fun name(): String

    fun kanSetteKilde(): Boolean = false

    fun harSkrivetilgang(): Boolean = false

    fun harLesetilgang(): Boolean = false
}

abstract class ExternalUser(val identifiedBy: TokenValidationContext) : User

class Self(private val prosess: String) : User {
    override fun name() = prosess

    override fun kanSetteKilde() = true
}

class SystemUser(identifiedBy: TokenValidationContext) : ExternalUser(identifiedBy) {
    override fun name(): String {
        return identifiedBy.hentTokenClaims(AZURE_ISSUER)
            ?.getStringClaim("azp_name") // format=cluster:namespace:app-name
            ?: throw IllegalArgumentException("Støtter ikke navn på systembruker")
    }

    override fun kanSetteKilde(): Boolean {
        return identifiedBy.hentTokenClaims(AZURE_ISSUER)!!.containsClaim("roles", "kan-sette-kilde")
    }

    override fun harSkrivetilgang(): Boolean = true

    override fun harLesetilgang(): Boolean = true
}

class HentEnhetException(override val detail: String, override val cause: Throwable?) :
    InternfeilException(detail, cause)

class SaksbehandlerMedEnheterOgRoller(
    identifiedBy: TokenValidationContext,
    private val navAnsattKlient: NavAnsattKlient,
    val saksbehandlerMedRoller: SaksbehandlerMedRoller,
) : ExternalUser(identifiedBy) {
    override fun name(): String {
        return identifiedBy.hentTokenClaims(AZURE_ISSUER)!!.getStringClaim("NAVident")
    }

    fun erSuperbruker() = name() in (saksbehandlereMedTilgangTilAlleEnheter)

    fun enheter() =
        if (saksbehandlerMedRoller.harRolleNasjonalTilgang()) {
            Enheter.nasjonalTilgangEnheter()
        } else {
            try {
                runBlocking {
                    navAnsattKlient.hentEnhetForSaksbehandler(name()).map { it.id }
                }
            } catch (e: Exception) {
                throw HentEnhetException("Henting av enheter feilet. Sjekk on-prem status(fss)", e)
            }
        }

    override fun harSkrivetilgang(): Boolean {
        val enheter = enheter()
        val skrivetilgangEnhetsnummere = Enheter.enheterMedSkrivetilgang()
        return enheter.any { skrivetilgangEnhetsnummere.contains(it) }
    }

    override fun harLesetilgang(): Boolean {
        val enheter = enheter()
        val lesetilgangEnheter = Enheter.enheterMedLesetilgang()
        return enheter.any { lesetilgangEnheter.contains(it) }
    }
}

fun decideUser(
    principal: TokenValidationContextPrincipal,
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
    navAnsattKlient: NavAnsattKlient,
    brukerTokenInfo: BrukerTokenInfo,
): ExternalUser {
    return if (principal.context.issuers.contains(AZURE_ISSUER)) {
        if (brukerTokenInfo is Systembruker) {
            SystemUser(principal.context)
        } else {
            SaksbehandlerMedEnheterOgRoller(
                principal.context,
                navAnsattKlient,
                SaksbehandlerMedRoller(brukerTokenInfo as Saksbehandler, saksbehandlerGroupIdsByKey),
            )
        }
    } else {
        throw IllegalStateException("no token from preapproved issuers")
    }
}

interface DatabaseKontekst {
    fun activeTx(): Connection

    fun <T> inTransaction(block: () -> T): T
}

fun <T> inTransaction(block: () -> T): T =
    Kontekst.get().databasecontxt.inTransaction {
        block()
    }

fun databaseContext() = Kontekst.get().databasecontxt
