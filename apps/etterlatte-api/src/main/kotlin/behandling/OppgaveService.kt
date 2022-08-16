package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
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

class OppgaveService(private val behandlingKlient: BehandlingKlient, private val vedtakKlient: VedtakKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentAlleOppgaver(accessToken: String): Oppgaver {
        logger.info("Henter alle oppgaver")

        return coroutineScope {
            behandlingKlient.hentOppgaver(accessToken).oppgaver
                .map { oppgave ->
                    async {
                        Oppgave(
                            behandlingsId = oppgave.behandlingId,
                            sakId = oppgave.sak.id,
                            status = oppgave.behandlingStatus,
                            oppgaveStatus = oppgave.oppgaveStatus,
                            soeknadType = oppgave.sak.sakType,
                            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, //todo dette kan nå hentes fra behandling
                            regdato = oppgave.regdato.toLocalDateTime().toString(),
                            fristdato = oppgave.fristDato.atStartOfDay().toString(),
                            fnr = oppgave.sak.ident,
                            beskrivelse = "",
                            saksbehandler = "",
                            handling = Handling.BEHANDLE,
                            antallSoesken = 2 // midlertidig kommentert ut for å komme videre i DEV.
//                            vedtakKlient.hentVedtak(
//                                oppgave.behandlingId.toString(),
//                                accessToken
//                            ).kommerSoekerTilgodeResultat?.familieforhold?.let { familieforhold ->
//                                hentAntallSøsken(familieforhold)
//                            }
                        )
                    }
                }
                .awaitAll()
                .let { Oppgaver(it) }
        }
    }
}

private fun hentAntallSøsken(familiemedlemmer: Familiemedlemmer): Int? {
    return familiemedlemmer.avdoed.barn?.let {
        (it.size - 1).coerceAtLeast(0)
    }
}
