package no.nav.etterlatte.oppgaveGosys

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.GosysOppgave
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
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
    ): Long

    suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: Tidspunkt,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long

    suspend fun ferdigstill(
        oppgaveId: String,
        oppgaveVersjon: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgave

    suspend fun feilregistrer(
        oppgaveId: String,
        request: FeilregistrerOppgaveRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
) : GosysOppgaveService {
    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<Long, GosysOppgave>()

    override suspend fun hentOppgaver(brukerTokenInfo: BrukerTokenInfo): List<GosysOppgave> {
        val saksbehandlerMedRoller = Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller

        val gosysOppgaver =
            gosysOppgaveKlient.hentOppgaver(
                enhetsnr = if (saksbehandlerMedRoller.harRolleStrengtFortrolig()) Enheter.STRENGT_FORTROLIG.enhetNr else null,
                brukerTokenInfo = brukerTokenInfo,
            )

        // Utveksle unike aktørIds til fnr for mapping
        val fnrByAktoerId =
            if (gosysOppgaver.oppgaver.isEmpty()) {
                emptyMap<String, String>()
            } else {
                val aktoerIds = gosysOppgaver.oppgaver.mapNotNull { it.aktoerId }.toSet()
                pdltjenesterKlient.hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds)
            }

        return gosysOppgaver.oppgaver
            .map { it.fraGosysOppgaveTilNy(fnrByAktoerId) }.filterForEnheter(Kontekst.get().AppUser)
    }

    private fun List<GosysOppgave>.filterForEnheter(bruker: User) = this.filterOppgaverForEnheter(bruker)

    private fun List<GosysOppgave>.filterOppgaverForEnheter(user: User) =
        this.filterForEnheter(
            user,
        ) { item, enheter ->
            enheter.contains(item.enhet)
        }

    fun List<GosysOppgave>.filterForEnheter(
        user: User,
        filter: (item: GosysOppgave, enheter: List<String>) -> Boolean,
    ) = when (user) {
        is SaksbehandlerMedEnheterOgRoller -> {
            val enheter = user.enheter()
            this.filter { filter(it, enheter) }
        }

        else -> this
    }

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgave {
        return cache.getIfPresent(id) ?: gosysOppgaveKlient.hentOppgave(id, brukerTokenInfo).let {
            it.fraGosysOppgaveTilNy(pdltjenesterKlient.hentFolkeregisterIdenterForAktoerIdBolk(setOf(it.aktoerId!!)))
        }.also { cache.put(id, it) }
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tilordnes: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        return gosysOppgaveKlient.tildelOppgaveTilSaksbehandler(
            oppgaveId,
            oppgaveVersjon,
            tilordnes,
            brukerTokenInfo,
        ).versjon
    }

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: Tidspunkt,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        return gosysOppgaveKlient.endreFrist(oppgaveId, oppgaveVersjon, nyFrist.toLocalDate(), brukerTokenInfo).versjon
    }

    override suspend fun ferdigstill(
        oppgaveId: String,
        oppgaveVersjon: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgave {
        return gosysOppgaveKlient.ferdigstill(oppgaveId, oppgaveVersjon, brukerTokenInfo).let {
            it.fraGosysOppgaveTilNy(pdltjenesterKlient.hentFolkeregisterIdenterForAktoerIdBolk(setOf(it.aktoerId!!)))
        }
    }

    override suspend fun feilregistrer(
        oppgaveId: String,
        request: FeilregistrerOppgaveRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        val endreStatusRequest =
            EndreStatusRequest(
                versjon = request.versjon.toString(),
                status = "FEILREGISTRERT",
                beskrivelse = request.beskrivelse,
            )

        return gosysOppgaveKlient.feilregistrer(oppgaveId, endreStatusRequest, brukerTokenInfo).versjon
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
                frist =
                    this.fristFerdigstillelse?.let { frist ->
                        Tidspunkt.ofNorskTidssone(frist, LocalTime.MIDNIGHT)
                    },
                fnr = fnrByAktoerId[this.aktoerId],
                gjelder = temaTilSakType[this.tema]!!.name,
                enhet = this.tildeltEnhetsnr,
                saksbehandler = this.tilordnetRessurs?.let { OppgaveSaksbehandler(it) },
                beskrivelse = this.beskrivelse,
                sakType = temaTilSakType[this.tema]!!,
                journalpostId = this.journalpostId,
            )
        }
    }
}
