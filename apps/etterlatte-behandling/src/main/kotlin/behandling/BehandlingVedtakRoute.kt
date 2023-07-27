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
import java.util.*

internal fun Route.behandlingVedtakRoute(
    behandlingsstatusService: BehandlingStatusService,
    oppgaveService: OppgaveServiceNy,
    behandlingService: BehandlingService,
    kanBrukeNyOppgaveliste: Boolean
) {
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
                    if (kanBrukeNyOppgaveliste) {
                        oppgaveService.lukkOppgaveUnderbehandlingOgLagNyMedType(
                            fattVedtak.vedtakOppgaveDTO,
                            OppgaveType.ATTESTERING
                        )
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
                    if (kanBrukeNyOppgaveliste) {
                        oppgaveService.lukkOppgaveUnderbehandlingOgLagNyMedType(
                            underkjennVedtakOppgave.vedtakOppgaveDTO,
                            OppgaveType.UNDERKJENT
                        )
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
                    if (kanBrukeNyOppgaveliste) {
                        oppgaveService.ferdigStillOppgaveUnderBehandling(
                            attesterVedtakOppgave.vedtakOppgaveDTO
                        )
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}