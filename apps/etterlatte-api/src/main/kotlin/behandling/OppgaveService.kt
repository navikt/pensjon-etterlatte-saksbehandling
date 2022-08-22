package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
import org.slf4j.LoggerFactory
import java.util.*

data class Oppgave(
    val behandlingsId: UUID,
    val sakId: Long,
    val status: BehandlingStatus?,
    val oppgaveStatus: OppgaveStatus?,
    val soeknadType: String,
    val behandlingType: BehandlingType,
    val regdato: String,
    val fristdato: String,
    val fnr: String,
    val beskrivelse: String,
    val saksbehandler: String,
    val handling: Handling,
    val antallSoesken: Int?
)

data class Oppgaver(val oppgaver: List<Oppgave>)

enum class Handling {
    BEHANDLE
}

class OppgaveService(private val behandlingKlient: BehandlingKlient, private val vedtakKlient: EtterlatteVedtak) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentAlleOppgaver(accessToken: String): Oppgaver {
        logger.info("Henter alle oppgaver")

        val vedtakListe = vedtakKlient.hentAlleVedtak(accessToken).associateBy { it.behandlingId }
        val oppgaver = behandlingKlient.hentOppgaver(accessToken).oppgaver
            .map { oppgave ->
                Oppgave(
                    behandlingsId = oppgave.behandlingId,
                    sakId = oppgave.sak.id,
                    status = oppgave.behandlingStatus,
                    oppgaveStatus = oppgave.oppgaveStatus,
                    soeknadType = oppgave.sak.sakType,
                    behandlingType = oppgave.behandlingsType,
                    regdato = oppgave.regdato.toLocalDateTime().toString(),
                    fristdato = oppgave.fristDato.atStartOfDay().toString(),
                    fnr = oppgave.sak.ident,
                    beskrivelse = "",
                    saksbehandler = "",
                    handling = Handling.BEHANDLE,
                    antallSoesken = vedtakListe[oppgave.behandlingId]!!.kommerSoekerTilgodeResultat?.familieforhold
                        ?.let { familieforhold -> hentAntallSøsken(familieforhold) }
                )
            }

        return Oppgaver(oppgaver)
    }
}

private fun hentAntallSøsken(familiemedlemmer: Familiemedlemmer): Int? {
    return familiemedlemmer.avdoed.barn?.let {
        (it.size - 1).coerceAtLeast(0)
    }
}