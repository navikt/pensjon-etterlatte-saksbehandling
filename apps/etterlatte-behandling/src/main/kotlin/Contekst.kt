package no.nav.etterlatte

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import tilgangsstyring.AzureGroup
import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst
)

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

class Saksbehandler(
    identifiedBy: TokenValidationContext,
    private val saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
    private val enhetService: EnhetService
) :
    ExternalUser(identifiedBy) {

    override fun name(): String {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.getStringClaim("NAVident")
    }

    fun harRolleSaksbehandler(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey[AzureGroup.SAKSBEHANDLER]
        )
    }

    fun harRolleAttestant(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey[AzureGroup.ATTESTANT]
        )
    }

    fun harRolleStrengtFortrolig(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey[AzureGroup.STRENGT_FORTROLIG]
        )
    }

    fun harRolleFortrolig(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey[AzureGroup.FORTROLIG]
        )
    }

    private fun harRolleNasjonalTilgang(): Boolean {
        val jwtTokenClaims = identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims
        return jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey[AzureGroup.NASJONAL_MED_LOGG]
        ) || jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey[AzureGroup.NASJONAL_UTEN_LOGG]
        )
    }

    fun enheter() = if (harRolleNasjonalTilgang()) {
        Enheter.nasjonalTilgangEnheter()
    } else {
        runBlocking {
            enhetService.enheterForIdent(name()).map { it.id }
        }
    }
}

class Kunde(identifiedBy: TokenValidationContext) : ExternalUser(identifiedBy) {
    override fun name(): String {
        return identifiedBy.getJwtToken("tokenx").jwtTokenClaims.getStringClaim("pid")
    }
}

fun decideUser(
    principal: TokenValidationContextPrincipal,
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
    enhetService: EnhetService
): ExternalUser {
    return if (principal.context.issuers.contains("tokenx")) {
        Kunde(principal.context)
    } else if (principal.context.issuers.contains(AZURE_ISSUER)) {
        if (principal.context.getJwtToken(AZURE_ISSUER).jwtTokenClaims.let { it.getStringClaim("oid") == it.subject }) {
            SystemUser(principal.context)
        } else {
            Saksbehandler(principal.context, saksbehandlerGroupIdsByKey, enhetService)
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