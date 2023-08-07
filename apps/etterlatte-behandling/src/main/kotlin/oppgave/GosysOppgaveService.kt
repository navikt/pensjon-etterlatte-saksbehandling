package no.nav.etterlatte.oppgave

import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.token.BrukerTokenInfo

interface GosysOppgaveService {
    suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<OppgaveDTO>
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val pdlKlient: PdlKlient,
    private val featureToggleService: FeatureToggleService
) :
    GosysOppgaveService {

    override suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<OppgaveDTO> {
        if (!featureToggleService.isEnabled(GosysOppgaveServiceFeatureToggle.HentGosysOppgaver, false)) {
            return emptyList()
        }

        val gosysOppgaver = gosysOppgaveKlient.hentOppgaver("PEN", "4808", brukerTokenInfo)

        // Utveksle unike aktørIds til fnr for mapping
        val fnrByAktoerId = if (gosysOppgaver.oppgaver.isEmpty()) {
            emptyMap<String, String>()
        } else {
            val aktoerIds = gosysOppgaver.oppgaver.map { it.aktoerId }.toSet()
            pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds)
        }

        return gosysOppgaver.oppgaver.map { OppgaveDTO.fraGosysOppgave(it, fnrByAktoerId) }
    }
}

private enum class GosysOppgaveServiceFeatureToggle(private val key: String) : FeatureToggle {
    HentGosysOppgaver("pensjon-etterlatte.hent-gosys-oppgaver");

    override fun key() = key
}