package no.nav.etterlatte.oppgaveGosys

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalTime

interface GosysOppgaveService {
    suspend fun hentOppgaver(
        saksbehandler: String?,
        tema: String?,
        enhetsnr: Enhetsnummer?,
        harTildeling: Boolean?,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave>

    suspend fun hentOppgaverForPerson(
        foedselsnummer: String,
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

    suspend fun flyttTilGjenny(
        oppgaveId: Long,
        request: FlyttOppgavetilGjennyRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): OppgaveIntern

    suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        request: SaksbehandlerEndringGosysRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long

    suspend fun ferdigstill(
        oppgaveId: String,
        brukerTokenInfo: BrukerTokenInfo,
        request: FerdigstillGosysOppgaveRequest,
    ): GosysOppgave

    suspend fun feilregistrer(
        oppgaveId: String,
        request: FeilregistrerOppgaveRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long
}

class GosysOppgaveServiceImpl(
    private val gosysOppgaveKlient: GosysOppgaveKlient,
    private val oppgaveService: OppgaveService,
    private val saksbehandlerService: SaksbehandlerService,
    private val saksbehandlerInfoDao: SaksbehandlerInfoDao,
    private val pdltjeneserKlient: PdlTjenesterKlient,
) : GosysOppgaveService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<Long, GosysOppgave>()

    private val saksbehandlerCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<String, Map<String, String>> { _ ->
                saksbehandlerInfoDao.hentAlleSaksbehandlere().associate {
                    it.ident to it.navn
                }
            }

    private fun hentEnheterForSaksbehandler(
        enhetsnr: Enhetsnummer?,
        ident: String,
    ): List<Enhetsnummer> {
        val saksbehandler = inTransaction { saksbehandlerService.hentKomplettSaksbehandler(ident) }
        return if (saksbehandler.kanSeOppgaveliste) {
            if (enhetsnr == null) {
                saksbehandler.enheter
            } else {
                return saksbehandler.enheter.filter { it == enhetsnr }
            }
        } else {
            emptyList()
        }
    }

    override suspend fun hentOppgaver(
        saksbehandler: String?,
        tema: String?,
        enhetsnr: Enhetsnummer?,
        harTildeling: Boolean?,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave> {
        val saksbehandlerMedRoller = Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
        val harRolleStrengtFortrolig = saksbehandlerMedRoller.harRolleStrengtFortrolig()

        val enheterSomSkalSoekesEtter =
            if (harRolleStrengtFortrolig) {
                listOf(Enheter.STRENGT_FORTROLIG.enhetNr)
            } else {
                hentEnheterForSaksbehandler(enhetsnr, brukerTokenInfo.ident())
            }

        val temaListe = if (tema.isNullOrBlank()) listOf("EYO", "EYB") else listOf(tema)

        val alleGosysOppgaver =
            coroutineScope {
                enheterSomSkalSoekesEtter
                    .map {
                        async {
                            gosysOppgaveKlient.hentOppgaver(
                                aktoerId = null,
                                saksbehandler = saksbehandler,
                                tema = temaListe,
                                enhetsnr = it,
                                harTildeling = harTildeling,
                                brukerTokenInfo = brukerTokenInfo,
                            )
                        }
                    }.map {
                        it.await()
                    }.flatMap { it.oppgaver }
            }
        val gosysOppgaver = GosysOppgaver(alleGosysOppgaver.size, alleGosysOppgaver)

        logger.info("Fant ${gosysOppgaver.antallTreffTotalt} oppgave(r) med tema: $temaListe")

        return inTransaction {
            gosysOppgaver.oppgaver
                .map { it.tilGosysOppgave() }
                .filterForEnheter(Kontekst.get().AppUser)
        }
    }

    override suspend fun hentOppgaverForPerson(
        foedselsnummer: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave> {
        val aktoerId =
            pdltjeneserKlient.hentAktoerId(foedselsnummer)?.aktoerId
                ?: throw IkkeFunnetException("AKTOER_ID_ER_NULL", "Klarte ikke hente aktørid for person")

        val gosysOppgaver =
            gosysOppgaveKlient.hentOppgaver(
                aktoerId = aktoerId,
                saksbehandler = null,
                tema = emptyList(), // hent alle oppgaver, uavhengig av tema
                enhetsnr = null,
                harTildeling = null,
                brukerTokenInfo = brukerTokenInfo,
            )

        logger.info("Fant ${gosysOppgaver.antallTreffTotalt} oppgave(r) på person ${foedselsnummer.maskerFnr()}")

        return inTransaction {
            gosysOppgaver.oppgaver
                .map { it.tilGosysOppgave() }
                .filterForEnheter(Kontekst.get().AppUser)
        }
    }

    override suspend fun hentJournalfoeringsoppgave(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<GosysOppgave> {
        val gosysOppgaver = gosysOppgaveKlient.hentJournalfoeringsoppgave(journalpostId, brukerTokenInfo)

        return inTransaction {
            gosysOppgaver.oppgaver
                .map { it.tilGosysOppgave() }
                .filterForEnheter(Kontekst.get().AppUser)
        }
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
    ): GosysOppgave =
        cache.getIfPresent(id) ?: gosysOppgaveKlient
            .hentOppgave(id, brukerTokenInfo)
            .run { inTransaction { tilGosysOppgave() } }
            .also { cache.put(id, it) }

    override suspend fun flyttTilGjenny(
        oppgaveId: Long,
        request: FlyttOppgavetilGjennyRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): OppgaveIntern {
        logger.info("Starter flytting av gosys-oppgave (id=$oppgaveId) til Gjenny")

        val gosysOppgave = hentOppgave(oppgaveId, brukerTokenInfo)

        if (gosysOppgave.oppgavetype !in listOf("JFR", "JFR_UT")) {
            logger.error("Fikk forespørsel om flytting av oppgavetype=${gosysOppgave.oppgavetype}. Burde det støttes?")
            throw StoetterKunFlyttingAvJournalfoeringsoppgave()
        }

        if (gosysOppgave.journalpostId.isNullOrBlank()) {
            throw UgyldigForespoerselException(
                code = "KAN_IKKE_FLYTTE_GOSYS_OPPGAVE",
                detail = "Kan ikke flytte oppgave når journalpostId mangler (oppgaveId=${gosysOppgave.id})",
            )
        }

        val nyOppgave =
            inTransaction {
                oppgaveService.opprettOppgave(
                    referanse = gosysOppgave.journalpostId!!,
                    sakId = SakId(request.sakid),
                    kilde = OppgaveKilde.SAKSBEHANDLER,
                    type = OppgaveType.JOURNALFOERING,
                    merknad = gosysOppgave.beskrivelse,
                    frist = gosysOppgave.frist,
                    saksbehandler = brukerTokenInfo.ident(),
                )
            }

        val feilregistrertOppgaveId =
            feilregistrer(
                oppgaveId.toString(),
                FeilregistrerOppgaveRequest(
                    beskrivelse = "Oppgave overført til Gjenny",
                    versjon = gosysOppgave.versjon,
                    enhetsnr = request.enhetsnr,
                ),
                brukerTokenInfo,
            )

        logger.info("Feilregistrerte Gosys-oppgave $feilregistrertOppgaveId")

        return nyOppgave
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        request: SaksbehandlerEndringGosysRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long =
        gosysOppgaveKlient
            .tildelOppgaveTilSaksbehandler(
                oppgaveId,
                request,
                brukerTokenInfo,
            ).versjon

    override suspend fun ferdigstill(
        oppgaveId: String,
        brukerTokenInfo: BrukerTokenInfo,
        request: FerdigstillGosysOppgaveRequest,
    ): GosysOppgave =
        gosysOppgaveKlient
            .ferdigstill(oppgaveId, request.versjon, brukerTokenInfo, request.enhetsnr)
            .run { inTransaction { tilGosysOppgave() } }

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
                endretAvEnhetsnr = request.enhetsnr,
            )

        return gosysOppgaveKlient.feilregistrer(oppgaveId, endreStatusRequest, brukerTokenInfo).id
    }

    private fun GosysApiOppgave.tilGosysOppgave(): GosysOppgave =
        GosysOppgave(
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
            enhet = this.tildeltEnhetsnr,
            saksbehandler =
                this.tilordnetRessurs?.let {
                    OppgaveSaksbehandler(
                        it,
                        saksbehandlerCache.get("ANY_KEY")[it] ?: it,
                    )
                },
            beskrivelse = this.beskrivelse,
            journalpostId = this.journalpostId,
            bruker = this.bruker,
        )
}

class StoetterKunFlyttingAvJournalfoeringsoppgave :
    UgyldigForespoerselException(
        code = "UGYLDIG_OPPGAVETYPE_FOR_FLYTTING",
        detail = "Støtter foreløpig kun flytting av journalføringsoppgaver",
    )
