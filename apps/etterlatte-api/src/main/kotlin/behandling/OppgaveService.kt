package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
import no.nav.etterlatte.libs.common.vikaar.PersoninfoAvdoed
import no.nav.etterlatte.typer.Sak
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
    val antallSoesken: Int?,
)

data class Oppgaver(val oppgaver: List<Oppgave>)

enum class Handling {
    BEHANDLE
}

data class SakMedBehandling(val sak: Sak, val behandling: BehandlingSammendrag)

class OppgaveService(private val behandlingKlient: BehandlingKlient, private val vedtakKlient: VedtakKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentAlleOppgaver(accessToken: String): Oppgaver {
        logger.info("Henter alle oppgaver")

        return Oppgaver(behandlingKlient.hentOppgaver(accessToken).oppgaver.map {
            coroutineScope {
                val vedtak = async { vedtakKlient.hentVedtak(it.behandlingId.toString(), accessToken) }

                Oppgave(
                    it.behandlingId,
                    it.sak.id,
                    it.behandlingStatus,
                    it.oppgaveStatus,
                    it.sak.sakType,
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    it.regdato.toLocalDateTime().toString(),
                    it.fristDato.atStartOfDay().toString(),
                    it.sak.ident,
                    "",
                    "",
                    Handling.BEHANDLE,
                    antallSoesken = vedtak.await().kommerSoekerTilgodeResultat?.familieforhold?.let { familieforhold ->
                        hentAntallSøsken(familieforhold)
                    }
                )
            }
        })
    }
}

private fun hentAntallSøsken(familiemedlemmer: Familiemedlemmer): Int? {
    return familiemedlemmer.avdoed.barn?.let {
        (it.size - 1).coerceAtLeast(0)
    }
}
