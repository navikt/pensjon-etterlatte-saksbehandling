package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import org.slf4j.LoggerFactory
import java.util.*

data class OppgaveDTO(
    val behandlingsId: UUID?,
    val sakId: Long,
    val status: BehandlingStatus?,
    val oppgaveStatus: OppgaveStatus?,
    val soeknadType: String,
    val behandlingType: BehandlingType,
    val oppgaveType: OppgaveType,
    val regdato: String,
    val fristdato: String,
    val fnr: String,
    val beskrivelse: String,
    val saksbehandler: String,
    val handling: Handling,
    val antallSoesken: Int
)

enum class OppgaveType {
    HENDELSE, FØRSTEGANGSBEHANDLING, REVURDERING, MANUELT_OPPHOER // ktlint-disable
}

private fun BehandlingType.tilOppgaveType(): OppgaveType = when (this) {
    BehandlingType.FØRSTEGANGSBEHANDLING -> OppgaveType.FØRSTEGANGSBEHANDLING
    BehandlingType.MANUELT_OPPHOER -> OppgaveType.MANUELT_OPPHOER
    BehandlingType.REVURDERING -> OppgaveType.REVURDERING
}

private fun GrunnlagsendringsType.tilBeskrivelse(): String = when (this) {
    GrunnlagsendringsType.UTFLYTTING -> "Utflytting fra Norge"
    GrunnlagsendringsType.DOEDSFALL -> "Dødsfall"
    GrunnlagsendringsType.FORELDER_BARN_RELASJON -> "Endring i forelder/barn-relasjon"
}

data class Oppgaver(val oppgaver: List<OppgaveDTO>)

enum class Handling {
    BEHANDLE, GAA_TIL_SAK
}

class OppgaveService(private val behandlingKlient: BehandlingKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentAlleOppgaver(accessToken: String): Oppgaver {
        logger.info("Henter alle oppgaver")

        val behandlingsoppgaver = behandlingKlient.hentOppgaver(accessToken).oppgaver
        val grunnlagsendringsoppgaver = behandlingKlient.hentUhaandterteGrunnlagshendelser(accessToken)

        val oppgaverGrunnlagsendringer = grunnlagsendringsoppgaver.map { grunnlagsendringsoppgave ->
            OppgaveDTO(
                behandlingsId = grunnlagsendringsoppgave.behandlingId,
                sakId = grunnlagsendringsoppgave.sakId,
                status = null,
                oppgaveStatus = OppgaveStatus.NY,
                soeknadType = grunnlagsendringsoppgave.sakType.name,
                behandlingType = BehandlingType.REVURDERING,
                oppgaveType = OppgaveType.HENDELSE,
                regdato = grunnlagsendringsoppgave.opprettet.toString(),
                fristdato = grunnlagsendringsoppgave.opprettet.plusMonths(1).toString(),
                fnr = grunnlagsendringsoppgave.bruker.value,
                beskrivelse = grunnlagsendringsoppgave.type.tilBeskrivelse(),
                saksbehandler = "",
                handling = Handling.GAA_TIL_SAK,
                antallSoesken = 0
            )
        }

        return Oppgaver(
            (
                behandlingsoppgaver.map {
                    OppgaveDTO(
                        behandlingsId = it.behandlingId,
                        sakId = it.sak.id,
                        status = it.behandlingStatus,
                        oppgaveStatus = it.oppgaveStatus,
                        soeknadType = it.sak.sakType,
                        behandlingType = it.behandlingsType,
                        oppgaveType = it.behandlingsType.tilOppgaveType(),
                        regdato = it.regdato.toLocalDateTime().toString(),
                        fristdato = it.fristDato.atStartOfDay().toString(),
                        fnr = it.sak.ident,
                        beskrivelse = "",
                        saksbehandler = "",
                        handling = Handling.BEHANDLE,
                        antallSoesken = it.antallSoesken
                    )
                } + oppgaverGrunnlagsendringer
                ).sortedByDescending { it.fristdato }
        )
    }
}