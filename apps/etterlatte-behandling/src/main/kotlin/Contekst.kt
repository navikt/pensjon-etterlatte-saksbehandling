package no.nav.etterlatte

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.SystemBruker
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import tilgangsstyring.AzureGroup
import tilgangsstyring.SaksbehandlerMedRoller
import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst
) {
    fun appUserAsSaksbehandler(): SaksbehandlerMedEnheterOgRoller {
        return this.AppUser as SaksbehandlerMedEnheterOgRoller
    }
}

interface User {
    fun name(): String
    fun kanSetteKilde(): Boolean = false
}

abstract class ExternalUser(val identifiedBy: TokenValidationContext) : User

class Self(val prosess: String) : User {
    override fun name() = prosess
    override fun kanSetteKilde() = true
}

class SystemUser(identifiedBy: TokenValidationContext) : ExternalUser(identifiedBy) {
    override fun name(): String {
        throw IllegalArgumentException("Støtter ikke navn på systembruker")
    }

    override fun kanSetteKilde(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim("roles", "kan-sette-kilde")
    }
}

class SaksbehandlerMedEnheterOgRoller(
    identifiedBy: TokenValidationContext,
    private val enhetService: EnhetService,
    val saksbehandlerMedRoller: SaksbehandlerMedRoller
) : ExternalUser(identifiedBy) {

    override fun name(): String {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.getStringClaim("NAVident")
    }

    fun enheter() = if (saksbehandlerMedRoller.harRolleNasjonalTilgang()) {
        Enheter.nasjonalTilgangEnheter()
    } else {
        runBlocking {
            enhetService.enheterForIdent(name()).map { it.id }
        }
    }
}

fun decideUser(
    principal: TokenValidationContextPrincipal,
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
    enhetService: EnhetService,
    bruker: Bruker
): ExternalUser {
    return if (principal.context.issuers.contains(AZURE_ISSUER)) {
        if (bruker is SystemBruker) {
            SystemUser(principal.context)
        } else {
            SaksbehandlerMedEnheterOgRoller(
                principal.context,
                enhetService,
                SaksbehandlerMedRoller(bruker as Saksbehandler, saksbehandlerGroupIdsByKey)
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

fun <T> inTransaction(block: () -> T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

fun databaseContext() = Kontekst.get().databasecontxt