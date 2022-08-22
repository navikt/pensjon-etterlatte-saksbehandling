package no.nav.etterlatte.ktortokenexchange

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

class LolSecMediator : SecurityContextMediator {
    private fun attachToRoute(route: Route) {
        route.intercept(ApplicationCallPipeline.Call) {
            withContext(
                Dispatchers.Default + ThreadBoundSecCtx.asContextElement(
                    value = object : SecurityContext {
                        override fun user(): String? = call.request.headers["Authorization"]!!
                    }
                )
            ) {
                proceed()
            }
        }
    }

    override fun outgoingToken(audience: String): suspend () -> String =
        { "pid: ${ThreadBoundSecCtx.get().user()}, aud: $audience" }
    override fun installSecurity(ktor: Application) {}

    override fun secureRoute(ctx: Route, block: Route.() -> Unit) {
        attachToRoute(ctx)
        ctx.block()
    }
}