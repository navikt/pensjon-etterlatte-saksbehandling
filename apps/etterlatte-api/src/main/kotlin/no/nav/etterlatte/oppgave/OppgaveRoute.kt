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
import no.nav.etterlatte.libs.ktor.route.sakId

fun Route.oppgaveRoute(oppgaveService: OppgaveService) {
    route("/api/v1/oppgave") {
        route("/ep-journalfoering") {
            install(AuthorizationPlugin) {
                accessPolicyRolesEllerAdGrupper = setOf("opprett-jfr-oppgave")
            }

            post {
                val request = call.receive<OpprettJournalfoeringOppgave>()

                oppgaveService.opprettOppgave(
                    sakId,
                    NyOppgaveDto(
                        oppgaveKilde = OppgaveKilde.EESSI,
                        oppgaveType = OppgaveType.JOURNALFOERING,
                        merknad = request.merknad,
                        referanse = request.journalpostId,
                    ),
                )
            }
        }
    }
}

internal data class OpprettJournalfoeringOppgave(
    val journalpostId: String,
    val merknad: String,
)
