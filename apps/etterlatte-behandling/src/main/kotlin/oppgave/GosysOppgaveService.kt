package no.nav.etterlatte.oppgave

import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import java.time.LocalTime
import java.util.*

interface GosysOppgaveService {
    suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<OppgaveNy>
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val pdlKlient: PdlKlient,
    private val featureToggleService: FeatureToggleService
) :
    GosysOppgaveService {
    override suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<OppgaveNy> {
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

        return gosysOppgaver.oppgaver.map { fraGosysOppgaveTilNy(it, fnrByAktoerId) }
    }

    companion object {
        private val temaTilSakType = mapOf(
            "PEN" to SakType.BARNEPENSJON, // tmp for testing FIXME remove when Oppgave API supports EYB/EYO
            "EYB" to SakType.BARNEPENSJON,
            "EYO" to SakType.OMSTILLINGSSTOENAD
        )

        private fun fraGosysOppgaveTilNy(gosysOppgave: GosysOppgave, fnrByAktoerId: Map<String, String?>): OppgaveNy {
            return gosysOppgave.let {
                OppgaveNy(
                    id = UUID.randomUUID(), // : UUID,
                    status = Status.NY, // tmp?
                    enhet = it.tildeltEnhetsnr,
                    sakId = 0L,
                    kilde = OppgaveKilde.EKSTERN,
                    type = OppgaveType.GOSYS,
                    saksbehandler = it.tilordnetRessurs,
                    referanse = null,
                    merknad = null,
                    opprettet = it.opprettetTidspunkt,
                    sakType = temaTilSakType[it.tema]!!,
                    fnr = fnrByAktoerId[it.aktoerId],
                    frist = Tidspunkt.ofNorskTidssone(dato = it.fristFerdigstillelse, tid = LocalTime.MIDNIGHT)
                )
            }
        }
    }
}

private enum class GosysOppgaveServiceFeatureToggle(private val key: String) : FeatureToggle {
    HentGosysOppgaver("pensjon-etterlatte.hent-gosys-oppgaver");

    override fun key() = key
}