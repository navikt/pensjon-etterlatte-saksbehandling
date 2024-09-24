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
import no.nav.etterlatte.libs.common.omregning.AutomatiskRevurderingResponse
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.time.LocalTime

fun Route.omregningRoutes(omregningService: OmregningService) {
    val logger = routeLogger

    route("/omregning") {
        post {
            val request = call.receive<Omregningshendelse>()
            kunSkrivetilgang(sakId = request.sakId) {
                val forrigeBehandling = inTransaction { omregningService.hentForrigeBehandling(request.sakId) }
                val persongalleri = omregningService.hentPersongalleri(forrigeBehandling.id)
                val revurderingOgOppfoelging =
                    inTransaction {
                        omregningService.opprettOmregning(
                            sakId = request.sakId,
                            fraDato = request.fradato,
                            revurderingAarsak = request.revurderingaarsak,
                            prosessType = request.prosesstype,
                            forrigeBehandling = forrigeBehandling,
                            persongalleri = persongalleri,
                            oppgavefrist = request.oppgavefrist?.let { Tidspunkt.ofNorskTidssone(it, LocalTime.NOON) },
                        )
                    }
                retryOgPakkUt { revurderingOgOppfoelging.leggInnGrunnlag() }
                retryOgPakkUt {
                    inTransaction {
                        revurderingOgOppfoelging.opprettOgTildelOppgave()
                    }
                }
                retryOgPakkUt { revurderingOgOppfoelging.sendMeldingForHendelse() }
                val behandlingId = revurderingOgOppfoelging.behandlingId()
                val sakType = revurderingOgOppfoelging.sakType()
                call.respond(AutomatiskRevurderingResponse(behandlingId, forrigeBehandling.id, sakType))
            }
        }

        put("kjoering") {
            val request = call.receive<KjoeringRequest>()
            logger.info("Motter hendelse om at regulering har status ${request.status.name} i sak ${request.sakId}")
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
