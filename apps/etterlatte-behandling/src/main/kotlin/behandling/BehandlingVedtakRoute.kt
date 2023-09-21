package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("behandlingVedtakRoute")

internal fun Route.behandlingVedtakRoute(
    behandlingsstatusService: BehandlingStatusService,
    oppgaveService: OppgaveService,
    behandlingService: BehandlingService,
) {
    fun haandterFeilIOppgaveService(e: Exception) {
        logger.error("Fikk en feil i ferdigstilling av oppgave som stopper ferdigstilling.", e)
        throw e
    }

    route("/fattvedtak") {
        post {
            val fattVedtak = call.receive<VedtakEndringDTO>()
            val behandling =
                behandlingService.hentBehandling(
                    UUID.fromString(fattVedtak.vedtakOppgaveDTO.referanse),
                )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settFattetVedtak(behandling, fattVedtak.vedtakHendelse)
                    try {
                        oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                            fattetoppgave = fattVedtak.vedtakOppgaveDTO,
                            oppgaveType = OppgaveType.ATTESTERING,
                            saksbehandler = brukerTokenInfo,
                            merknad = fattVedtak.vedtakHendelse.kommentar,
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
            val behandling =
                behandlingService.hentBehandling(
                    UUID.fromString(underkjennVedtakOppgave.vedtakOppgaveDTO.referanse),
                )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settReturnertVedtak(behandling, underkjennVedtakOppgave.vedtakHendelse)
                    val merknadFraAttestant =
                        underkjennVedtakOppgave.vedtakHendelse.let {
                            listOfNotNull(it.valgtBegrunnelse, it.kommentar).joinToString(separator = ": ")
                        }
                    try {
                        oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                            fattetoppgave = underkjennVedtakOppgave.vedtakOppgaveDTO,
                            oppgaveType = OppgaveType.UNDERKJENT,
                            merknad = merknadFraAttestant,
                            saksbehandler = brukerTokenInfo,
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
            val behandling =
                behandlingService.hentBehandling(
                    UUID.fromString(attesterVedtakOppgave.vedtakOppgaveDTO.referanse),
                )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settAttestertVedtak(behandling, attesterVedtakOppgave.vedtakHendelse)
                    try {
                        oppgaveService.ferdigStillOppgaveUnderBehandling(
                            behandlingEllerHendelseId = attesterVedtakOppgave.vedtakOppgaveDTO.referanse,
                            saksbehandler = brukerTokenInfo,
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
