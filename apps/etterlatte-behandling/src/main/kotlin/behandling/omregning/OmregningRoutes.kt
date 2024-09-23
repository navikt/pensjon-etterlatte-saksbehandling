package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.omregning.OpprettOmregningResponse
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.time.LocalDate
import java.time.LocalTime

data class ManuellOmregningDto(
    val fraDato: LocalDate,
)

fun Route.omregningRoutes(omregningService: OmregningService) {
    val logger = routeLogger

    route("/api/omregning") {
        post("manuell/{$SAKID_CALL_PARAMETER}") {
            logger.info("Manuell omregning av sak $sakId")
            val request = call.receive<ManuellOmregningDto>()

            // TODO nekte hvis åpen behandling?
            // TODO nekte hvis overstyrt beregning?

            kunSkrivetilgang(sakId = sakId) {
                val forrigeBehandling = inTransaction { omregningService.hentForrigeBehandling(sakId) }
                omregningService.opprettOmregningManuelt(sakId, request.fraDato, forrigeBehandling)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/omregning") {
        post {
            val request = call.receive<Omregningshendelse>()
            kunSkrivetilgang(sakId = request.sakId) {
                val forrigeBehandling = inTransaction { omregningService.hentForrigeBehandling(request.sakId) }
                val revurderingOgOppfoelging =
                    omregningService.opprettOmregning(
                        sakId = request.sakId,
                        fraDato = request.fradato,
                        revurderingAarsak = request.revurderingaarsak,
                        prosessType = request.prosesstype,
                        forrigeBehandling = forrigeBehandling,
                        oppgavefrist = request.oppgavefrist?.let { Tidspunkt.ofNorskTidssone(it, LocalTime.NOON) },
                    )
                val behandlingId = revurderingOgOppfoelging.behandlingId()
                val sakType = revurderingOgOppfoelging.sakType()
                call.respond(OpprettOmregningResponse(behandlingId, forrigeBehandling.id, sakType))
            }
        }

        put("kjoering") {
            val request = call.receive<KjoeringRequest>()
            logger.info("Motter hendelse om at omregning har status ${request.status.name} i sak ${request.sakId}")
            inTransaction {
                omregningService.oppdaterKjoering(request, brukerTokenInfo)
            }
            call.respond(HttpStatusCode.OK)
        }

        post("kjoeringFullfoert") {
            val request = call.receive<LagreKjoeringRequest>()
            inTransaction {
                omregningService.kjoeringFullfoert(request)
            }
            call.respond(HttpStatusCode.Created)
        }
    }
}
