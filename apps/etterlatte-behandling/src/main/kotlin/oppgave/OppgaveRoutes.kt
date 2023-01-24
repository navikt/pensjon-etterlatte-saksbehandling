package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import java.util.*

internal fun Route.oppgaveRoutes(repo: OppgaveDao) {
    route("/oppgaver") {
        get {
            val bruker = Kontekst.get().AppUser

            if (bruker is Saksbehandler) {
                val oppgaver = repo.finnOppgaverForRoller(
                    listOfNotNull(
                        Rolle.SAKSBEHANDLER.takeIf { bruker.harRolleSaksbehandler() },
                        Rolle.ATTESTANT.takeIf { bruker.harRolleAttestant() }
                    )
                ).map {
                    OppgaveDTO(
                        behandlingId = it.behandlingId,
                        sakId = it.sak.id,
                        status = it.behandlingStatus,
                        oppgaveStatus = it.oppgaveStatus,
                        soeknadType = it.sak.sakType.toString(),
                        behandlingType = it.behandlingsType,
                        regdato = it.regdato.toLocalDateTime().toString(),
                        fristdato = it.fristDato.atStartOfDay().toString(),
                        fnr = it.sak.ident,
                        beskrivelse = "",
                        saksbehandler = "",
                        handling = Handling.BEHANDLE,
                        antallSoesken = it.antallSoesken
                    )
                }.let { OppgaveListeDto(it) }

                call.respond(oppgaver)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

data class OppgaveListeDto(
    val oppgaver: List<OppgaveDTO>
)

data class OppgaveDTO(
    val behandlingId: UUID,
    val sakId: Long,
    val status: BehandlingStatus,
    val oppgaveStatus: OppgaveStatus?,
    val soeknadType: String,
    val behandlingType: BehandlingType,
    val regdato: String,
    val fristdato: String,
    val fnr: String,
    val beskrivelse: String,
    val saksbehandler: String,
    val handling: Handling,
    val antallSoesken: Int
)

enum class Handling {
    BEHANDLE
}