package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.soeknad.SoeknadType
import org.slf4j.LoggerFactory
import java.util.*

data class Oppgave(
    val behandlingsId: UUID,
    val sakId: Long,
    val status: BehandlingStatus,
    val soeknadType: String,
    val behandlingType: BehandlingType,
    val regdato: Date,
    val fristdato: Date,
    val fnr: String,
    val beskrivelse: String,
    val saksbehandler: String,
    val handling: Handling,
)

data class Oppgaver(val oppgaver: List<Oppgave>)

enum class Handling {
    BEHANDLE
}

data class SakMedBehandling(val sak: Sak, val behandling: BehandlingSammendrag)

class OppgaveService(private val behandlingKlient: BehandlingKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentAlleOppgaver(accessToken: String): Oppgaver {
        logger.info("Henter alle oppgaver")

        val saker = behandlingKlient.hentSaker(accessToken).saker

        val sakerMedBehandling: List<SakMedBehandling> = saker.flatMap {
            mapBehandlingerTilSak(
                it,
                behandlingKlient.hentBehandlingerForSak(it.sakId.toInt(), accessToken)
            )
        }

        val oppgaveListe: List<Oppgave> = sakerMedBehandling.map { mapTilOppgave(it) }
        return Oppgaver(oppgaver = oppgaveListe)
    }

    companion object Utils {
        fun mapBehandlingerTilSak(sak: Sak, behandlinger: BehandlingerSammendrag): List<SakMedBehandling> {
            return behandlinger.behandlinger.map { SakMedBehandling(sak, it) }
        }

        fun mapTilOppgave(sakMedBehandling: SakMedBehandling): Oppgave {
            return Oppgave(
                behandlingsId = sakMedBehandling.behandling.id,
                sakId = sakMedBehandling.sak.sakId,
                status = sakMedBehandling.behandling.status,
                soeknadType = sakMedBehandling.sak.sakType,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, //må hentes ut etterhvert
                regdato = Date(),
                fristdato = Date(), //pluss intervall
                fnr = sakMedBehandling.sak.ident,
                beskrivelse = "",
                saksbehandler = "",
                handling = Handling.BEHANDLE //logikk basert på status her
            )
        }
    }
}