package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.VedtakEndringDTO
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("behandlingVedtakRoute")

internal fun Route.behandlingVedtakRoute(
    behandlingsstatusService: BehandlingStatusService,
    oppgaveService: OppgaveServiceNy,
    behandlingService: BehandlingService,
    kanBrukeNyOppgaveliste: Boolean
) {
    fun haandterFeilIOppgaveService(e: Exception) {
        if (kanBrukeNyOppgaveliste) {
            logger.error("Fikk en feil i ferdigstilling av oppgave som stopper ferdigstilling: ", e)
            throw e
        } else {
            logger.error(
                "Fikk en feil i ferdigstilling av oppgave som svelges, siden oppgave " +
                    "ikke er skrudd på: ",
                e
            )
        }
    }

    route("/fattvedtak") {
        post {
            val fattVedtak = call.receive<VedtakEndringDTO>()
            val behandling = behandlingService.hentBehandling(
                UUID.fromString(fattVedtak.vedtakOppgaveDTO.referanse)
            )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settFattetVedtak(behandling, fattVedtak.vedtakHendelse)
                    try {
                        oppgaveService.lukkOppgaveUnderbehandlingOgLagNyMedType(
                            fattVedtak.vedtakOppgaveDTO,
                            OppgaveType.ATTESTERING,
                            fattVedtak.vedtakHendelse.saksbehandler
                        )
                    } catch (e: Exception) {
                        haandterFeilIOppgaveService(e)
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
    route("/underkjennvedtak") {
        post {
            val underkjennVedtakOppgave = call.receive<VedtakEndringDTO>()
            val behandling = behandlingService.hentBehandling(
                UUID.fromString(underkjennVedtakOppgave.vedtakOppgaveDTO.referanse)
            )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settReturnertVedtak(behandling, underkjennVedtakOppgave.vedtakHendelse)
                    try {
                        oppgaveService.lukkOppgaveUnderbehandlingOgLagNyMedType(
                            underkjennVedtakOppgave.vedtakOppgaveDTO,
                            OppgaveType.UNDERKJENT,
                            underkjennVedtakOppgave.vedtakHendelse.saksbehandler
                        )
                    } catch (e: Exception) {
                        haandterFeilIOppgaveService(e)
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
    route("/attestervedtak") {
        post {
            val attesterVedtakOppgave = call.receive<VedtakEndringDTO>()
            val behandling = behandlingService.hentBehandling(
                UUID.fromString(attesterVedtakOppgave.vedtakOppgaveDTO.referanse)
            )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settAttestertVedtak(behandling, attesterVedtakOppgave.vedtakHendelse)
                    try {
                        oppgaveService.ferdigStillOppgaveUnderBehandling(
                            attesterVedtakOppgave.vedtakOppgaveDTO.referanse,
                            attesterVedtakOppgave.vedtakHendelse.saksbehandler
                        )
                    } catch (e: Exception) {
                        haandterFeilIOppgaveService(e)
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}