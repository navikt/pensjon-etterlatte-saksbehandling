package no.nav.etterlatte

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst
)

interface User{
    fun name():String
}
abstract class ExternalUser(val identifiedBy: TokenValidationContext): User

class Self(val prosess: String): User{
    override fun name() = prosess
}
class SystemUser(identifiedBy: TokenValidationContext): ExternalUser(identifiedBy) {
    override fun name(): String {
        return identifiedBy.getJwtToken("azure").jwtTokenClaims.getStringClaim("")
    }
}

class Saksbehandler(identifiedBy: TokenValidationContext): ExternalUser(identifiedBy) {
    override fun name(): String {
        return identifiedBy.getJwtToken("azure").jwtTokenClaims.getStringClaim("NAVident")
    }
}

class Kunde(identifiedBy: TokenValidationContext): ExternalUser(identifiedBy){
    override fun name(): String {
        return identifiedBy.getJwtToken("tokenx").jwtTokenClaims.getStringClaim("pid")
    }
}


fun decideUser(principal: TokenValidationContextPrincipal): ExternalUser{
    return if(principal.context.issuers.contains("tokenx")){
        Kunde(principal.context)
    } else if(principal.context.issuers.contains("azure")) {
        if(principal.context.getJwtToken("azure").jwtTokenClaims.let { it.getStringClaim("oid") == it.subject }){
            SystemUser(principal.context)
        }else{
            Saksbehandler(principal.context)
        }
    } else throw IllegalStateException("no token from preapproved issuers")
}


interface DatabaseKontekst{
    fun activeTx(): Connection
    fun <T> inTransaction(block: ()->T): T
}


fun databaseContext() = Kontekst.get().databasecontxt