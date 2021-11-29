package io.ktor.auth

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.routing.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.ktortokenexchange.SecurityContext
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediator
import no.nav.etterlatte.ktortokenexchange.ThreadBoundSecCtx

class LolSecMediator: SecurityContextMediator {
    private fun attachToRoute(route: Route) {
        route.intercept(ApplicationCallPipeline.Call) {
            withContext(
                Dispatchers.Default + ThreadBoundSecCtx.asContextElement(
                    value = object: SecurityContext {
                        override fun user(): String? = call.request.headers["Authorization"]!!
                    }
                )
            ) {
                proceed()
            }
        }    }

    override fun outgoingToken(audience: String): suspend () -> String = { "pid: ${ThreadBoundSecCtx.get().user()}, aud: $audience" }
    override fun installSecurity(ktor: Application) {}

    override fun secureRoute(ctx: Route, block: Route.() -> Unit) {
        attachToRoute(ctx)
        ctx.block()
    }

}