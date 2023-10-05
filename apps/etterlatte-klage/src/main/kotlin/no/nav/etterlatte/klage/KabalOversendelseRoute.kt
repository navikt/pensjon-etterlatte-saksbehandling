package no.nav.etterlatte.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.KlageOversendelseDto
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.medBody

enum class KlageFeaturetoggle(val key: String) : FeatureToggle {
    KanBrukeKlage("pensjon-etterlatte.kan-bruke-generell-behandling") {
        override fun key(): String {
            return key
        }
    },
}

fun Route.kabalOvesendelseRoute(
    kabalOversendelseService: KabalOversendelseService,
    featureToggleService: FeatureToggleService,
) {
    suspend fun PipelineContext<Unit, ApplicationCall>.hvisEnabled(
        toggle: FeatureToggle,
        block: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit,
    ) {
        if (!featureToggleService.isEnabled(toggle, false)) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            block()
        }
    }

    route("api") {
        route("send-klage") {
            post {
                hvisEnabled(KlageFeaturetoggle.KanBrukeKlage) {
                    kunSystembruker {
                        medBody<KlageOversendelseDto> {
                            kabalOversendelseService.sendTilKabal(it.klage, it.ekstraData)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }
        }
    }
}
