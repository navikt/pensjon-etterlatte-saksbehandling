package no.nav.etterlatte

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory
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
    private val saksbehandlerGroupIdsByKey: Map<String, String>,
    private val enhetService: EnhetService
) :
    ExternalUser(identifiedBy) {
    private val logger = LoggerFactory.getLogger(Saksbehandler::class.java)

    init {
        logger.info("""Groups: ${identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.getAsList("groups")}""")
    }

    override fun name(): String {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.getStringClaim("NAVident")
    }

    fun harRolleSaksbehandler(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey["AZUREAD_SAKSBEHANDLER_GROUPID"]
        )
    }

    fun harRolleAttestant(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey["AZUREAD_ATTESTANT_GROUPID"]
        )
    }

    fun harRolleStrengtFortrolig(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey["AZUREAD_STRENGT_FORTROLIG_GROUPID"]
        )
    }

    fun harRolleFortrolig(): Boolean {
        return identifiedBy.getJwtToken(AZURE_ISSUER).jwtTokenClaims.containsClaim(
            "groups",
            saksbehandlerGroupIdsByKey["AZUREAD_FORTROLIG_GROUPID"]
        )
    }

    fun enheter() = runBlocking {
        enhetService.enheterForIdent(name())
    }
}

class Kunde(identifiedBy: TokenValidationContext) : ExternalUser(identifiedBy) {
    override fun name(): String {
        return identifiedBy.getJwtToken("tokenx").jwtTokenClaims.getStringClaim("pid")
    }
}

fun decideUser(
    principal: TokenValidationContextPrincipal,
    saksbehandlerGroupIdsByKey: Map<String, String>,
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