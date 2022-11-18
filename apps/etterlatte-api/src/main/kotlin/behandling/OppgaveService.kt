package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
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

        val behandlingsoppgaver = behandlingKlient.hentOppgaver(accessToken).oppgaver
        val vedtakListe = vedtakKlient.hentVedtakBolk(
            behandlingsidenter = behandlingsoppgaver.map { it.behandlingId.toString() },
            accessToken = accessToken
        ).associateBy { it.behandlingId }

        return Oppgaver(
            behandlingsoppgaver.map {
                Oppgave(
                    behandlingsId = it.behandlingId,
                    sakId = it.sak.id,
                    status = it.behandlingStatus,
                    oppgaveStatus = it.oppgaveStatus,
                    soeknadType = it.sak.sakType,
                    behandlingType = it.behandlingsType,
                    regdato = it.regdato.toLocalDateTime().toString(),
                    fristdato = it.fristDato.atStartOfDay().toString(),
                    fnr = it.sak.ident,
                    beskrivelse = "",
                    saksbehandler = "",
                    handling = Handling.BEHANDLE,
                    antallSoesken = 0
                    // TODO dette må kanskje hentes fra behandling i stedet?
                    // antallSoesken = vedtakListe[it.behandlingId]?.kommerSoekerTilgodeResultat?.familieforhold
                    //    ?.let { familieforhold -> hentAntallSøsken(familieforhold) }
                )
            }
        )
    }
}

/*
private fun hentAntallSøsken(familiemedlemmer: Familiemedlemmer): Int? {
    return familiemedlemmer.avdoed.barn?.let {
        (it.size - 1).coerceAtLeast(0)
    }
}*/