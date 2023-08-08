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
    suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<OppgaveDTO>
    suspend fun hentOppgaverNy(brukerTokenInfo: BrukerTokenInfo): List<OppgaveNy>
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val pdlKlient: PdlKlient,
    private val featureToggleService: FeatureToggleService
) :
    GosysOppgaveService {
    override suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<OppgaveDTO> {
        return hentOppgaverInternal(brukerTokenInfo, GosysOppgaveServiceImpl::fraGosysOppgaverTilDto)
    }

    override suspend fun hentOppgaverNy(brukerTokenInfo: BrukerTokenInfo): List<OppgaveNy> {
        return hentOppgaverInternal(brukerTokenInfo, GosysOppgaveServiceImpl::fraGosysOppgaverTilNy)
    }

    private suspend fun <T> hentOppgaverInternal(
        brukerTokenInfo: BrukerTokenInfo,
        mapper: (List<GosysOppgave>, Map<String, String?>) -> List<T>
    ): List<T> {
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

        return mapper.invoke(gosysOppgaver.oppgaver, fnrByAktoerId)
    }

    companion object {
        private val temaTilSakType = mapOf(
            "PEN" to SakType.BARNEPENSJON, // tmp for testing FIXME remove when Oppgave API supports EYB/EYO
            "EYB" to SakType.BARNEPENSJON,
            "EYO" to SakType.OMSTILLINGSSTOENAD
        )

        private fun fraGosysOppgaverTilDto(
            gosysoppgaver: List<GosysOppgave>,
            fnrByAktoerId: Map<String, String?>
        ): List<OppgaveDTO> {
            return gosysoppgaver.map { OppgaveDTO.fraGosysOppgave(it, fnrByAktoerId) }
        }

        private fun fraGosysOppgaverTilNy(
            gosysoppgaver: List<GosysOppgave>,
            fnrByAktoerId: Map<String, String?>
        ): List<OppgaveNy> {
            return gosysoppgaver.map { fraGosysOppgaveTilNy(it, fnrByAktoerId) }
        }

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