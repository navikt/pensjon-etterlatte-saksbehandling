package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("behandlingVedtakRoute")

internal fun Route.behandlingVedtakRoute(
    behandlingsstatusService: BehandlingStatusService,
    oppgaveService: OppgaveService,
    behandlingService: BehandlingService,
) {
    fun haandterFeilIOppgaveService(e: Exception) {
        logger.error("Fikk en feil i ferdigstilling av oppgave.", e)
        throw e
    }

    route("/fattvedtak") {
        post {
            val vedtak = call.receive<VedtakEndringDTO>()

            kunSkrivetilgang(sakId = vedtak.sakIdOgReferanse.sakId) {
                val behandling =
                    inTransaction {
                        behandlingService.hentBehandling(
                            UUID.fromString(vedtak.sakIdOgReferanse.referanse),
                        )
                    }
                if (behandling == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
                } else {
                    inTransaction {
                        behandlingsstatusService.settFattetVedtak(behandling, vedtak, brukerTokenInfo)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
    route("/underkjennvedtak") {
        post {
            val underkjennVedtakOppgave = call.receive<VedtakEndringDTO>()

            kunSkrivetilgang(sakId = underkjennVedtakOppgave.sakIdOgReferanse.sakId) {
                val behandling =
                    inTransaction {
                        behandlingService.hentBehandling(
                            UUID.fromString(underkjennVedtakOppgave.sakIdOgReferanse.referanse),
                        )
                    }
                if (behandling == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
                } else {
                    inTransaction {
                        behandlingsstatusService.settReturnertVedtak(behandling, underkjennVedtakOppgave)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
    route("/attestervedtak") {
        post {
            val attesterVedtakOppgave = call.receive<VedtakEndringDTO>()

            kunSkrivetilgang(sakId = attesterVedtakOppgave.sakIdOgReferanse.sakId) {
                val behandling =
                    inTransaction {
                        behandlingService.hentBehandling(
                            UUID.fromString(attesterVedtakOppgave.sakIdOgReferanse.referanse),
                        )
                    }
                if (behandling == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
                } else {
                    inTransaction {
                        behandlingsstatusService.settAttestertVedtak(behandling, attesterVedtakOppgave, brukerTokenInfo)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
