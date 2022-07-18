package no.nav.etterlatte

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst
)

interface User{
    fun name():String
    fun kanSetteKilde():Boolean = false
}
abstract class ExternalUser(val identifiedBy: TokenValidationContext): User

class Self(val prosess: String): User{
    override fun name() = prosess
    override fun kanSetteKilde() = true
}
class SystemUser(identifiedBy: TokenValidationContext): ExternalUser(identifiedBy) {
    override fun name(): String {
        throw IllegalArgumentException("Støtter ikke navn på systembruker")
    }
    override fun kanSetteKilde(): Boolean{
        return identifiedBy.getJwtToken("azure").jwtTokenClaims.containsClaim("roles", "kan-sette-kilde")
    }
}

class Saksbehandler(identifiedBy: TokenValidationContext): ExternalUser(identifiedBy) {
    init {
        println("""Groups: ${identifiedBy.getJwtToken("azure").jwtTokenClaims.getAsList("groups")}""")
    }
    override fun name(): String {
        return identifiedBy.getJwtToken("azure").jwtTokenClaims.getStringClaim("NAVident")
    }

    fun harRolleSaksbehandler(): Boolean{
        return identifiedBy.getJwtToken("azure").jwtTokenClaims.containsClaim("groups", "8bb9b8d1-f46a-4ade-8ee8-5895eccdf8cf")
    }
    fun harRolleAttestant(): Boolean{
        return identifiedBy.getJwtToken("azure").jwtTokenClaims.containsClaim("groups", "63f46f74-84a8-4d1c-87a8-78532ab3ae60")
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
fun <T> inTransaction(block: () -> T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

fun databaseContext() = Kontekst.get().databasecontxt