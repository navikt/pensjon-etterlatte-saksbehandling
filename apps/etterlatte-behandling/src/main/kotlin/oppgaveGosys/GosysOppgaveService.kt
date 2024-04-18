package no.nav.etterlatte.oppgaveGosys

import GosysOppgave
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalTime

interface GosysOppgaveService {
    suspend fun hentOppgaver(
        saksbehandler: String?,
        tema: String?,
        enhet: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave>

    suspend fun hentJournalfoeringsoppgave(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave>

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
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<Long, GosysOppgave>()

    override suspend fun hentOppgaver(
        saksbehandler: String?,
        tema: String?,
        enhet: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave> {
        val saksbehandlerMedRoller = Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller

        val gosysOppgaver =
            gosysOppgaveKlient.hentOppgaver(
                saksbehandler = saksbehandler,
                tema = if (tema.isNullOrBlank()) listOf("EYO", "EYB") else listOf(tema),
                enhetsnr = if (saksbehandlerMedRoller.harRolleStrengtFortrolig()) Enheter.STRENGT_FORTROLIG.enhetNr else enhet,
                brukerTokenInfo = brukerTokenInfo,
            )

        logger.info("Fant ${gosysOppgaver.antallTreffTotalt} oppgave(r) med tema: $tema")

        // Utveksle unike aktørIds til fnr for mapping
        val fnrByAktoerId =
            if (gosysOppgaver.oppgaver.isEmpty()) {
                emptyMap<String, String>()
            } else {
                val aktoerIds = gosysOppgaver.oppgaver.mapNotNull { it.aktoerId }.toSet()
                pdltjenesterKlient.hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds)
            }

        return gosysOppgaver.oppgaver
            .map { it.fraGosysOppgaveTilNy(fnrByAktoerId) }
            .filterForEnheter(Kontekst.get().AppUser)
    }

    override suspend fun hentJournalfoeringsoppgave(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave> {
        val gosysOppgaver = gosysOppgaveKlient.hentJournalfoeringsoppgave(journalpostId, brukerTokenInfo)

        // Utveksle unike aktørIds til fnr for mapping
        val fnrByAktoerId =
            if (gosysOppgaver.oppgaver.isEmpty()) {
                emptyMap<String, String>()
            } else {
                val aktoerIds = gosysOppgaver.oppgaver.mapNotNull { it.aktoerId }.toSet()
                pdltjenesterKlient.hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds)
            }

        return gosysOppgaver.oppgaver
            .map { it.fraGosysOppgaveTilNy(fnrByAktoerId) }
            .filterForEnheter(Kontekst.get().AppUser)
    }

    private fun List<GosysOppgave>.filterForEnheter(user: User) =
        when (user) {
            is SaksbehandlerMedEnheterOgRoller -> {
                val enheter = user.enheter()

                this.filter { it.enhet in enheter }
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
        private fun GosysApiOppgave.fraGosysOppgaveTilNy(fnrByAktoerId: Map<String, String?>): GosysOppgave {
            return GosysOppgave(
                id = this.id,
                versjon = this.versjon,
                status = this.status,
                tema = this.tema,
                oppgavetype = this.oppgavetype,
                opprettet = this.opprettetTidspunkt,
                frist =
                    this.fristFerdigstillelse?.let { frist ->
                        Tidspunkt.ofNorskTidssone(frist, LocalTime.MIDNIGHT)
                    },
                fnr = fnrByAktoerId[this.aktoerId],
                enhet = this.tildeltEnhetsnr,
                saksbehandler = this.tilordnetRessurs?.let { OppgaveSaksbehandler(it, it) },
                beskrivelse = this.beskrivelse,
                journalpostId = this.journalpostId,
            )
        }
    }
}
