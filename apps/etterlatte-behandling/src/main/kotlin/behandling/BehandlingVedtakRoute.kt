package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgaveNy.AttesterVedtakOppgave
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import java.util.*

internal fun Route.behandlingVedtakRoute(
    behandlingsstatusService: BehandlingStatusService,
    oppgaveService: OppgaveServiceNy,
    behandlingService: BehandlingService
) {
    route("/fattvedtak-behandling") {
        post {
            val attesterVedtakOppgave = call.receive<AttesterVedtakOppgave>()
            val behandling = behandlingService.hentBehandling(
                UUID.fromString(attesterVedtakOppgave.attesteringsOppgave.referanse)
            )
            if (behandling == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ingen behandling")
            } else {
                inTransaction {
                    behandlingsstatusService.settFattetVedtak(behandling, attesterVedtakOppgave.vedtakHendelse)
                    oppgaveService.haandterFattetvedtak(attesterVedtakOppgave.attesteringsOppgave)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}