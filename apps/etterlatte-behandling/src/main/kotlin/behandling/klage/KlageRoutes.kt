package no.nav.etterlatte.behandling.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.KLAGEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.klageId
import no.nav.etterlatte.libs.common.sakId

enum class KlageFeatureToggle(private val key: String) : FeatureToggle {
    KanBrukeKlageToggle("pensjon-etterlatte.kan-bruke-klage");

    override fun key(): String = key
}

internal fun Route.klageRoutes(klageService: KlageService, featureToggleService: FeatureToggleService) {
    suspend fun PipelineContext<Unit, ApplicationCall>.kunHvisEnabled(
        toggle: FeatureToggle,
        block: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit
    ) {
        if (!featureToggleService.isEnabled(toggle, false)) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            block()
        }
    }

    route("/api/klage") {
        post("opprett/{$SAKID_CALL_PARAMETER}") {
            kunHvisEnabled(KlageFeatureToggle.KanBrukeKlageToggle) {
                val sakId = sakId
                val klage = inTransaction {
                    klageService.opprettKlage(sakId)
                }
                call.respond(klage)
            }
        }

        get("{$KLAGEID_CALL_PARAMETER}") {
            kunHvisEnabled(KlageFeatureToggle.KanBrukeKlageToggle) {
                val klage = inTransaction {
                    klageService.hentKlage(klageId)
                }
                when (klage) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(klage)
                }
            }
        }

        get("sak/{$SAKID_CALL_PARAMETER}") {
            kunHvisEnabled(KlageFeatureToggle.KanBrukeKlageToggle) {
                val klager = inTransaction {
                    klageService.hentKlagerISak(sakId)
                }
                call.respond(klager)
            }
        }
    }
}