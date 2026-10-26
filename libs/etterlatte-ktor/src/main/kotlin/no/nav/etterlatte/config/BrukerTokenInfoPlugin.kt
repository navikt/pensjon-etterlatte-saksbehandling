package no.nav.etterlatte.config

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

val requestBrukerTokenInfo = ThreadLocal<BrukerTokenInfo?>()

private object BrukerTokenInfoHook : Hook<(ApplicationCall) -> BrukerTokenInfo> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: (ApplicationCall) -> BrukerTokenInfo,
    ) {
        // Call-fasen kjører etter autentisering. Bindingen følger coroutinen og gjenopprettes ved avslutning.
        pipeline.intercept(ApplicationCallPipeline.Call) {
            withContext(requestBrukerTokenInfo.asContextElement(handler(call))) {
                proceed()
            }
        }
    }
}

val BrukerTokenInfoPlugin =
    createRouteScopedPlugin("BrukerTokenInfoPlugin") {
        on(BrukerTokenInfoHook) { call ->
            call.brukerTokenInfo
        }
    }
