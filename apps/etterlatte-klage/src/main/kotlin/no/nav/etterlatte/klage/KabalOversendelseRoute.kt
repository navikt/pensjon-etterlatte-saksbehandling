package no.nav.etterlatte.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.KlageOversendelseDto
import no.nav.etterlatte.libs.common.hvisEnabled
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.medBody

enum class KlageFeaturetoggle(val key: String) : FeatureToggle {
    KanBrukeKlage("pensjon-etterlatte.kan-bruke-klage") {
        override fun key(): String {
            return key
        }
    },
}

fun Route.kabalOvesendelseRoute(
    kabalOversendelseService: KabalOversendelseService,
    featureToggleService: FeatureToggleService,
) {
    route("api") {
        route("send-klage") {
            post {
                hvisEnabled(featureToggleService, KlageFeaturetoggle.KanBrukeKlage) {
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
