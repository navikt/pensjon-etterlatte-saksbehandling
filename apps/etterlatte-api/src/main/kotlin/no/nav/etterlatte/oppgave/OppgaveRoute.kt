package no.nav.etterlatte.oppgave

import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId

fun Route.oppgaveRoute(oppgaveService: OppgaveService) {
    route("/api/v1/oppgave") {
        route("/journalfoering") {
            install(AuthorizationPlugin) {
                accessPolicyRolesEllerAdGrupper = setOf("opprett-jfr-oppgave")
            }

            post {
                val request = call.receive<OpprettJournalfoeringOppgave>()

                oppgaveService.opprettOppgave(
                    sakId = request.sakId,
                    NyOppgaveDto(
                        oppgaveKilde = OppgaveKilde.EESSI,
                        oppgaveType = OppgaveType.JOURNALFOERING,
                        merknad = request.beskrivelse,
                        referanse = request.journalpostId,
                    ),
                )
            }
        }
    }
}

internal data class OpprettJournalfoeringOppgave(
    val sakId: SakId,
    val journalpostId: String,
    val beskrivelse: String,
)
