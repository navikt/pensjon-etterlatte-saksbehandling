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
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.oppgave.domain.Handling
import no.nav.etterlatte.oppgave.domain.Oppgave
import no.nav.etterlatte.oppgave.domain.OppgaveType
import org.slf4j.LoggerFactory
import java.util.*

internal fun Route.oppgaveRoutes(service: OppgaveService) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/api/oppgaver") {
        get {
            when (val bruker = Kontekst.get().AppUser) {
                is Saksbehandler -> {
                    val finnOppgaverForBruker = service.finnOppgaverForBruker(bruker)
                    logger.info("Antall oppgaver totalt: ${finnOppgaverForBruker.size}")
                    val tilOppgaveListeDto = tilOppgaveListeDto(finnOppgaverForBruker)
                    logger.info("Antall oppgaver etter mapping: ${finnOppgaverForBruker.size}")
                    call.respond(tilOppgaveListeDto)
                }
                else -> call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

fun tilOppgaveListeDto(oppgaver: List<Oppgave>): OppgaveListeDto {
    return oppgaver.map {
        OppgaveDTO.fraOppgave(it)
    }.let { OppgaveListeDto(it) }
}

data class OppgaveListeDto(
    val oppgaver: List<OppgaveDTO>
)

data class OppgaveDTO(
    val behandlingId: UUID?,
    val sakId: Long,
    val status: BehandlingStatus?,
    val oppgaveStatus: OppgaveStatus?,
    val soeknadType: String,
    val oppgaveType: OppgaveType,
    val regdato: String,
    val fristdato: String,
    val fnr: String,
    val beskrivelse: String,
    val saksbehandler: String,
    val handling: Handling,
    val antallSoesken: Int?
) {
    companion object {
        fun fraOppgave(oppgave: Oppgave): OppgaveDTO {
            return when (oppgave) {
                is Oppgave.Grunnlagsendringsoppgave -> OppgaveDTO(
                    behandlingId = null,
                    sakId = oppgave.sakId,
                    status = BehandlingStatus.OPPRETTET,
                    oppgaveStatus = oppgave.oppgaveStatus,
                    soeknadType = oppgave.sakType.toString(),
                    oppgaveType = oppgave.oppgaveType,
                    regdato = oppgave.registrertDato.toLocalDateTime().toString(),
                    fristdato = oppgave.fristDato.atStartOfDay().toString(),
                    fnr = oppgave.fnr.value,
                    beskrivelse = oppgave.beskrivelse,
                    saksbehandler = "",
                    handling = oppgave.handling,
                    antallSoesken = null
                )
                is Oppgave.BehandlingOppgave -> OppgaveDTO(
                    behandlingId = oppgave.behandlingId,
                    sakId = oppgave.sakId,
                    status = oppgave.behandlingStatus,
                    oppgaveStatus = oppgave.oppgaveStatus,
                    soeknadType = oppgave.sakType.toString(),
                    oppgaveType = oppgave.oppgaveType,
                    regdato = oppgave.registrertDato.toLocalDateTime().toString(),
                    fristdato = oppgave.fristDato.atStartOfDay().toString(),
                    fnr = oppgave.fnr.value,
                    beskrivelse = "",
                    saksbehandler = "",
                    handling = oppgave.handling,
                    antallSoesken = oppgave.antallSoesken
                )
            }
        }
    }
}