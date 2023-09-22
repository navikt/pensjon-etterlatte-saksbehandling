package no.nav.etterlatte.oppgaveGosys

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.GosysOppgave
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import java.time.Duration
import java.time.LocalTime

interface GosysOppgaveService {
    suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<GosysOppgave>

    suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgave?

    suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tilordnes: String,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: Tidspunkt,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val pdlKlient: PdlKlient,
    private val featureToggleService: FeatureToggleService,
) : GosysOppgaveService {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<Long, GosysOppgave>()

    override suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<GosysOppgave> {
        if (!featureToggleService.isEnabled(GosysOppgaveServiceFeatureToggle.HentGosysOppgaver, false)) {
            return emptyList()
        }

        val gosysOppgaver =
            gosysOppgaveKlient.hentOppgaver(
                brukerTokenInfo = brukerTokenInfo,
            )

        // Utveksle unike aktørIds til fnr for mapping
        val fnrByAktoerId =
            if (gosysOppgaver.oppgaver.isEmpty()) {
                emptyMap<String, String>()
            } else {
                val aktoerIds = gosysOppgaver.oppgaver.mapNotNull { it.aktoerId }.toSet()
                pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds)
            }

        return gosysOppgaver.oppgaver.map { it.fraGosysOppgaveTilNy(fnrByAktoerId) }
    }

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgave? {
        return cache.getIfPresent(id) ?: gosysOppgaveKlient.hentOppgave(id, brukerTokenInfo).let {
            it.fraGosysOppgaveTilNy(pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(setOf(it.aktoerId)))
        }.also { cache.put(id, it) }
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tilordnes: String,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        gosysOppgaveKlient.tildelOppgaveTilSaksbehandler(oppgaveId, oppgaveVersjon, tilordnes, brukerTokenInfo)
    }

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: Tidspunkt,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        gosysOppgaveKlient.endreFrist(oppgaveId, oppgaveVersjon, nyFrist.toLocalDate(), brukerTokenInfo)
    }

    companion object {
        private val temaTilSakType =
            mapOf(
                "PEN" to SakType.BARNEPENSJON,
                "EYB" to SakType.BARNEPENSJON,
                "EYO" to SakType.OMSTILLINGSSTOENAD,
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
                sakType = temaTilSakType[this.tema]!!,
            )
        }
    }
}

private enum class GosysOppgaveServiceFeatureToggle(private val key: String) : FeatureToggle {
    HentGosysOppgaver("pensjon-etterlatte.hent-gosys-oppgaver"),
    ;

    override fun key() = key
}
