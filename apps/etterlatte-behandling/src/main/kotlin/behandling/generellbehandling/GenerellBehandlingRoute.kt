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
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.ktor.route.GENERELLBEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.generellBehandlingId
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sak.SakIkkeFunnetException
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import org.slf4j.LoggerFactory

internal fun Route.generellbehandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    sakService: SakService,
) {
    val logger = LoggerFactory.getLogger("GenerelBehandlingRoute")

    post("/api/generellbehandling/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
            val request = call.receive<OpprettGenerellBehandlingRequest>()
            val sak =
                inTransaction { sakService.finnSak(sakId) }
                    ?: throw SakIkkeFunnetException("Sak med id=$sakId finnes ikke")
            inTransaction {
                generellBehandlingService.opprettBehandling(
                    GenerellBehandling.opprettFraType(request.type, sakId),
                    saksbehandler,
                )
            }
            logger.info(
                "Opprettet generell behandling for sak ${sak.id} av typen ${request.type}",
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    put("/api/generellbehandling/sendtilattestering/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandlerMedSkrivetilgang { saksbehandler ->
            val request = call.receive<GenerellBehandling>()
            if (request.sakId != sakId) {
                throw SakParameterStemmerIkkeException(
                    "Innsendt sak har id=${request.sakId}, men sak i url er $sakId",
                )
            }
            inTransaction {
                generellBehandlingService.sendTilAttestering(request, saksbehandler)
            }
            logger.info(
                "Sender generell behandling med id=${request.id} til attestering i sak $sakId av typen ${request.type}",
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
                generellBehandlingService.underkjenn(generellBehandlingId, saksbehandler, kommentar, sakId)
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
                    sakId,
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
                generellBehandlingService.avbrytBehandling(generellBehandlingId, sakId, brukerTokenInfo)
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
            val hentetBehandling =
                inTransaction { generellBehandlingService.hentBehandlingMedId(generellBehandlingId) }
                    ?: throw GenerellIkkeFunnetException()
            call.respond(hentetBehandling)
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

class SakParameterStemmerIkkeException(
    override val detail: String,
) : UgyldigForespoerselException("SAK_PARAMETER_STEMER_IKKE", detail)
