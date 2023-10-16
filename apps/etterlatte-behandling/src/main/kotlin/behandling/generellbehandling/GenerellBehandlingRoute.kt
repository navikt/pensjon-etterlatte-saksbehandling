package no.nav.etterlatte.behandling.generellbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.GENERELLBEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.generellBehandlingId
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.sak.SakService

enum class GenerellBehandlingToggle(private val key: String) : FeatureToggle {
    KanBrukeGenerellBehandlingToggle("pensjon-etterlatte.kan-bruke-generell-behandling"),
    ;

    override fun key(): String = key
}

internal fun Route.generellbehandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    sakService: SakService,
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
    val logger = application.log

    post("/api/generellbehandling/{$SAKID_CALL_PARAMETER}") {
        hvisEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle) {
            kunSaksbehandler {
                val request = call.receive<OpprettGenerellBehandlingRequest>()
                val finnSak = inTransaction { sakService.finnSak(sakId) }
                if (finnSak == null) {
                    call.respond(HttpStatusCode.NotFound, "Saken finnes ikke")
                }
                inTransaction {
                    generellBehandlingService.opprettBehandling(
                        GenerellBehandling.opprettFraType(request.type, sakId),
                    )
                }
                logger.info(
                    "Opprettet generell behandling for sak $sakId av typen ${request.type}",
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    put("/api/generellbehandling/sendtilattestering/{$SAKID_CALL_PARAMETER}") {
        hvisEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle) {
            kunSaksbehandler { saksbehandler ->
                val request = call.receive<GenerellBehandling>()
                inTransaction {
                    generellBehandlingService.sendTilAttestering(request, saksbehandler)
                }
                logger.info(
                    "Opprettet generell behandling for sak $sakId av typen ${request.type}",
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    put("/api/generellbehandling/attester/{$SAKID_CALL_PARAMETER}/{$GENERELLBEHANDLINGID_CALL_PARAMETER}") {
        hvisEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle) {
            val generellBehandlingId = generellBehandlingId
            kunSaksbehandler { saksbehandler ->
                inTransaction {
                    generellBehandlingService.attester(generellBehandlingId, saksbehandler)
                }
                logger.info("Attester generell behandling med id $generellBehandlingId")
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    put("/api/generellbehandling/oppdater/{$SAKID_CALL_PARAMETER}") {
        hvisEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle) {
            kunSaksbehandler {
                val request = call.receive<GenerellBehandling>()
                inTransaction {
                    generellBehandlingService.lagreNyeOpplysninger(
                        request,
                    )
                }
                logger.info(
                    "Oppdatert generell behandling for sak $sakId av typen ${request.type}",
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    get("/api/generellbehandling/hent/{$GENERELLBEHANDLINGID_CALL_PARAMETER}") {
        hvisEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle) {
            kunSaksbehandler {
                val generellBehandlingId = generellBehandlingId
                val hentetBehandling = inTransaction { generellBehandlingService.hentBehandlingMedId(generellBehandlingId) }
                call.respond(hentetBehandling ?: HttpStatusCode.NotFound)
            }
        }
    }
    get("/api/generellbehandlingForSak/{$SAKID_CALL_PARAMETER}") {
        hvisEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle) {
            kunSaksbehandler {
                call.respond(inTransaction { generellBehandlingService.hentBehandlingForSak(sakId) })
            }
        }
    }
}

data class OpprettGenerellBehandlingRequest(
    val type: GenerellBehandling.GenerellBehandlingType,
)
