package no.nav.etterlatte.oppgave

import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.GosysOppgave
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import java.time.LocalTime

interface GosysOppgaveService {
    suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<GosysOppgave>
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val pdlKlient: PdlKlient,
    private val featureToggleService: FeatureToggleService
) :
    GosysOppgaveService {
    override suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<GosysOppgave> {
        if (!featureToggleService.isEnabled(GosysOppgaveServiceFeatureToggle.HentGosysOppgaver, false)) {
            return emptyList()
        }

        // tmp for testing FIXME remove hardcoding when Oppgave API supports EYB/EYO
        val gosysOppgaver = gosysOppgaveKlient.hentOppgaver("PEN", "4808", brukerTokenInfo)

        // Utveksle unike aktørIds til fnr for mapping
        val fnrByAktoerId = if (gosysOppgaver.oppgaver.isEmpty()) {
            emptyMap<String, String>()
        } else {
            val aktoerIds = gosysOppgaver.oppgaver.map { it.aktoerId }.toSet()
            pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds)
        }

        return gosysOppgaver.oppgaver.map { it.fraGosysOppgaveTilNy(fnrByAktoerId) }
    }

    companion object {
        private val temaTilSakType = mapOf(
            "PEN" to SakType.BARNEPENSJON, // tmp for testing FIXME remove when Oppgave API supports EYB/EYO
            "EYB" to SakType.BARNEPENSJON,
            "EYO" to SakType.OMSTILLINGSSTOENAD
        )

        private fun GosysApiOppgave.fraGosysOppgaveTilNy(fnrByAktoerId: Map<String, String?>): GosysOppgave {
            return GosysOppgave(
                id = this.id,
                versjon = this.versjon,
                status = Status.NY,
                opprettet = this.opprettetTidspunkt,
                frist = Tidspunkt.ofNorskTidssone(dato = this.fristFerdigstillelse, tid = LocalTime.MIDNIGHT),
                fnr = fnrByAktoerId[this.aktoerId]!!,
                gjelder = temaTilSakType[this.tema]!!.name,
                enhet = this.tildeltEnhetsnr,
                saksbehandler = this.tilordnetRessurs,
                beskrivelse = this.beskrivelse,
                sakType = temaTilSakType[this.tema]!!
            )
        }
    }
}

private enum class GosysOppgaveServiceFeatureToggle(private val key: String) : FeatureToggle {
    HentGosysOppgaver("pensjon-etterlatte.hent-gosys-oppgaver");

    override fun key() = key
}