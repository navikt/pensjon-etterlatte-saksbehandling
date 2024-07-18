package no.nav.etterlatte.behandling.generellbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.ktor.route.GENERELLBEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.generellBehandlingId
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang

internal fun Route.generellbehandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    sakService: SakService,
) {
    val logger = routeLogger

    post("/api/generellbehandling/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
            val request = call.receive<OpprettGenerellBehandlingRequest>()
            val finnSak = inTransaction { sakService.finnSak(sakId) }
            if (finnSak == null) {
                call.respond(HttpStatusCode.NotFound, "Saken finnes ikke")
            }
            inTransaction {
                generellBehandlingService.opprettBehandling(
                    GenerellBehandling.opprettFraType(request.type, sakId),
                    saksbehandler,
                )
            }
            logger.info(
                "Opprettet generell behandling for sak $sakId av typen ${request.type}",
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    put("/api/generellbehandling/sendtilattestering/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
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

    post("/api/generellbehandling/attester/{$SAKID_CALL_PARAMETER}/{$GENERELLBEHANDLINGID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
            inTransaction {
                generellBehandlingService.attester(generellBehandlingId, saksbehandler)
            }
            logger.info("Attester generell behandling med id $generellBehandlingId")
            call.respond(HttpStatusCode.OK)
        }
    }

    post("/api/generellbehandling/underkjenn/{$SAKID_CALL_PARAMETER}/{$GENERELLBEHANDLINGID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
            val kommentar = call.receive<Kommentar>()
            inTransaction {
                generellBehandlingService.underkjenn(generellBehandlingId, saksbehandler, kommentar)
            }
            logger.info("underkjent generell behandling med id $generellBehandlingId")
            call.respond(HttpStatusCode.OK)
        }
    }

    put("/api/generellbehandling/oppdater/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang {
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

    put("/api/generellbehandling/avbryt/{$SAKID_CALL_PARAMETER}/{$GENERELLBEHANDLINGID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang {
            inTransaction {
                generellBehandlingService.avbrytBehandling(generellBehandlingId, brukerTokenInfo)
            }
            logger.info(
                "Setter generell behandling med behandlingid $generellBehandlingId til avbrutt",
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    get("/api/generellbehandling/hent/{$GENERELLBEHANDLINGID_CALL_PARAMETER}") {
        kunSaksbehandler {
            val generellBehandlingId = generellBehandlingId
            val hentetBehandling = inTransaction { generellBehandlingService.hentBehandlingMedId(generellBehandlingId) }
            call.respond(hentetBehandling ?: HttpStatusCode.NotFound)
        }
    }

    get("/api/generellbehandling/hentforsak/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandler {
            call.respond(inTransaction { generellBehandlingService.hentBehandlingerForSak(sakId) })
        }
    }

    get("/api/generellbehandling/kravpakkeForSak/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandler {
            call.respond(generellBehandlingService.hentKravpakkeForSak(sakId, brukerTokenInfo))
        }
    }
}

data class OpprettGenerellBehandlingRequest(
    val type: GenerellBehandling.GenerellBehandlingType,
)
