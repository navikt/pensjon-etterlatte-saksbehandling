package no.nav.etterlatte.ktortokenexchange

import com.typesafe.config.Config
import io.ktor.application.Application
import io.ktor.auth.LolSecMediator
import io.ktor.config.HoconApplicationConfig
import io.ktor.routing.Route

object ThreadBoundSecCtx : ThreadLocal<SecurityContext>()

interface SecurityContext{
    fun user(): String?
}

interface SecurityContextMediator{
    fun outgoingToken(audience: String): suspend ()->String
    fun installSecurity(ktor: Application)
    fun secureRoute(ctx: Route, block: Route.()->Unit)
}

object SecurityContextMediatorFactory{
    fun from(config: Config): SecurityContextMediator {
        return when (config.getStringSafely("no.nav.etterlatte.sikkerhet")) {
            "ingen" -> LolSecMediator()
            else -> TokenSupportSecurityContextMediator(HoconApplicationConfig(config))
        }
    }
}

private fun Config.getStringSafely(path: String): String?{
    return if (hasPath(path)) getString(path) else null
}

fun Route.secureRoutUsing(ctx: SecurityContextMediator, route: Route.()->Unit){
    ctx.secureRoute(this, route)
}
fun Application.installAuthUsing(ctx: SecurityContextMediator){
    ctx.installSecurity(this)
}

