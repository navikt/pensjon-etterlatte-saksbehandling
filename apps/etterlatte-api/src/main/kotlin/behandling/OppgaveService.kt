package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.typer.Sak
import org.slf4j.LoggerFactory
import java.util.*

data class Oppgave(
    val behandlingsId: UUID,
    val sakId: Long,
    val status: BehandlingStatus?,
    val soeknadType: String,
    val behandlingType: BehandlingType,
    val regdato: String,
    val fristdato: String,
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

        return Oppgaver(behandlingKlient.hentOppgaver(accessToken).oppgaver.map {
            Oppgave(
                it.behandlingId,
                it.sak.id,
                it.behandlingStatus,
                it.sak.sakType,
                BehandlingType.FØRSTEGANGSBEHANDLING,
                it.regdato.toString(),
                it.fristDato.toString(),
                it.sak.ident,
                "",
                "",
                Handling.BEHANDLE
            )
        })
    }
}