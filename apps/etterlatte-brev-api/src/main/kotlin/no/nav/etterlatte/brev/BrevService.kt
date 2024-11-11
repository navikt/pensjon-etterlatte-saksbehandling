package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDistribusjonResponse
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.brev.model.FerdigstillJournalFoerOgDistribuerOpprettetBrev
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.brev.oppgave.OppgaveService
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.vedtaksbrev.UgyldigAntallMottakere
import no.nav.etterlatte.brev.vedtaksbrev.UgyldigMottakerKanIkkeFerdigstilles
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

class BrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val pdfGenerator: PDFGenerator,
    private val distribuerer: Brevdistribuerer,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogger = sikkerlogger()

    suspend fun opprettJournalfoerOgDistribuerRiver(
        bruker: BrukerTokenInfo,
        req: OpprettJournalfoerOgDistribuerRequest,
    ): BrevDistribusjonResponse {
        val (brev, enhetsnummer) =
            brevoppretter.opprettBrevSomHarInnhold(
                sakId = req.sakId,
                behandlingId = null,
                bruker = bruker,
                brevKode = req.brevKode,
                brevData = req.brevParametereAutomatisk.brevDataMapping(),
            )
        val brevId = brev.id

        try {
            pdfGenerator.ferdigstillOgGenererPDF(
                brevId,
                bruker,
                avsenderRequest = { _, _, _ ->
                    AvsenderRequest(
                        saksbehandlerIdent = req.avsenderRequest.saksbehandlerIdent,
                        attestantIdent = req.avsenderRequest.attestantIdent,
                        sakenhet = enhetsnummer,
                    )
                },
                brevKodeMapping = { req.brevKode },
                brevDataMapping = { ManueltBrevMedTittelData(it.innholdMedVedlegg.innhold(), it.tittel) },
            )

            logger.info("Journalfører brev med id: $brevId")
            journalfoerBrevService.journalfoer(brevId, bruker)

            logger.info("Distribuerer brev med id: $brevId")
            distribuerer.distribuer(brevId, bruker = bruker)

            logger.info("Brevid: $brevId er distribuert")
            return BrevDistribusjonResponse(brevId, true)
        } catch (e: Exception) {
            val oppdatertBrev = db.hentBrev(brevId)
            logger.error("Feil opp sto under ferdigstill/journalfør/distribuer av brevID=$brevId, status: ${oppdatertBrev.status}", e)
            oppgaveService.opprettOppgaveForFeiletBrev(req.sakId, brevId, bruker)
            return BrevDistribusjonResponse(brevId, false)
        }
    }

    suspend fun ferdigstillBrevJournalfoerOgDistribuerforOpprettetBrev(
        req: FerdigstillJournalFoerOgDistribuerOpprettetBrev,
        bruker: BrukerTokenInfo,
    ): BrevStatusResponse {
        val brevId = req.brevId
        val hentBrev = db.hentBrev(brevId)
        try {
            val brevStatus = hentBrev.status
            if (brevStatus.ikkeFerdigstilt()) {
                pdfGenerator.ferdigstillOgGenererPDF(
                    brevId,
                    bruker,
                    avsenderRequest = { _, _, _ ->
                        AvsenderRequest(
                            saksbehandlerIdent = req.avsenderRequest.saksbehandlerIdent,
                            attestantIdent = req.avsenderRequest.attestantIdent,
                            sakenhet = req.enhetsnummer,
                        )
                    },
                    brevKodeMapping = { hentBrev.brevkoder!! },
                    brevDataMapping = { ManueltBrevMedTittelData(it.innholdMedVedlegg.innhold(), it.tittel) },
                )
            }

            if (brevStatus.ikkeJournalfoert()) {
                logger.info("Journalfører brev med id: $brevId")
                journalfoerBrevService.journalfoer(brevId, bruker)
            }

            if (brevStatus.ikkeDistribuert()) {
                logger.info("Distribuerer brev med id: $brevId")
                distribuerer.distribuer(brevId, bruker = bruker)
            }

            logger.info("Brevid: $brevId er distribuert")
            val oppdatertBrev = db.hentBrev(brevId)
            return BrevStatusResponse(brevId, oppdatertBrev.status)
        } catch (e: Exception) {
            val oppdatertBrev = db.hentBrev(brevId)
            logger.error("Feil opp sto under ferdigstill/journalfør/distribuer av brevID=$brevId, status: ${oppdatertBrev.status}", e)

            return BrevStatusResponse(brevId, oppdatertBrev.status)
        }
    }

    fun hentBrev(id: BrevID): Brev = db.hentBrev(id)

    fun hentBrevForSak(sakId: SakId): List<Brev> = db.hentBrevForSak(sakId)

    suspend fun opprettNyttManueltBrev(
        sakId: SakId,
        bruker: BrukerTokenInfo,
        brevkode: Brevkoder,
        brevData: BrevDataRedigerbar,
    ): Brev =
        brevoppretter
            .opprettBrevSomHarInnhold(
                sakId = sakId,
                behandlingId = null,
                bruker = bruker,
                brevKode = brevkode,
                brevData = brevData,
            ).first

    data class BrevPayload(
        val hoveddel: Slate?,
        val vedlegg: List<BrevInnholdVedlegg>?,
    )

    fun hentBrevPayload(id: BrevID): BrevPayload {
        val hoveddel =
            db
                .hentBrevPayload(id)
                .also { logger.info("Hentet payload for brev (id=$id)") }

        val vedlegg =
            db
                .hentBrevPayloadVedlegg(id)
                .also { logger.info("Hentet payload til vedlegg for brev (id=$id)") }

        return BrevPayload(hoveddel, vedlegg)
    }

    fun lagreBrevPayload(
        id: BrevID,
        payload: Slate,
        bruker: BrukerTokenInfo,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterPayload(id, payload, bruker)
            .also { logger.info("Payload for brev (id=$id) oppdatert") }
    }

    fun lagreBrevPayloadVedlegg(
        id: BrevID,
        payload: List<BrevInnholdVedlegg>,
        bruker: BrukerTokenInfo,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterPayloadVedlegg(id, payload, bruker)
            .also { logger.info("Vedlegg payload for brev (id=$id) oppdatert") }
    }

    fun opprettMottaker(brevId: BrevID): Mottaker {
        val brev = sjekkOmBrevKanEndres(brevId)

        if (brev.mottakere.size > 1) {
            throw MaksAntallMottakere()
        }

        val nyMottaker = tomMottaker(type = MottakerType.KOPI)

        logger.info("Oppretter ny mottaker på brev=$brevId")

        db
            .opprettMottaker(brevId, nyMottaker)
            .also { logger.info("Ny mottaker opprettet på brev id=$brevId") }

        return nyMottaker
    }

    fun slettMottaker(
        brevId: BrevID,
        mottakerId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val brev = sjekkOmBrevKanEndres(brevId)

        logger.info("Sletter mottaker (id=$mottakerId) fra brev=$brevId")

        val mottaker = brev.mottakere.find { it.id == mottakerId }
        if (mottaker?.type == MottakerType.HOVED) {
            throw KanIkkeSletteHovedmottaker()
        } else if (brev.mottakere.size <= 1) {
            throw MinstEnMottakerPaakrevd()
        } else {
            db.slettMottaker(brevId, mottakerId, bruker)

            logger.info("Mottaker (id=$mottakerId) slettet fra brev=$brevId")
        }
    }

    fun oppdaterMottaker(
        brevId: BrevID,
        mottaker: Mottaker,
        bruker: BrukerTokenInfo,
    ): Int {
        val brev = sjekkOmBrevKanEndres(brevId)

        val lagretMottaker = brev.mottakere.single { it.id == mottaker.id }
        if (lagretMottaker.type != mottaker.type) {
            throw InternfeilException("Kan ikke sette hoved-/kopimottaker på vanlig oppdatering av mottaker")
        }

        logger.info("Oppdaterer mottaker (id=${mottaker.id}) på brev=$brevId")

        return db
            .oppdaterMottaker(brevId, mottaker, bruker)
            .also { logger.info("Mottaker på brev (id=$brevId) oppdatert") }
    }

    fun settHovedmottaker(
        brevId: BrevID,
        mottakerId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val brev = sjekkOmBrevKanEndres(brevId)

        if (brev.mottakere.find { it.id == mottakerId }?.type == MottakerType.HOVED) {
            return // Ikke gjør noe hvis mottakeren allerede er hovedmottaker
        }

        brev.mottakere
            .forEach {
                if (it.id == mottakerId) {
                    db.oppdaterMottaker(brevId, it.copy(type = MottakerType.HOVED), bruker)
                } else {
                    db.oppdaterMottaker(brevId, it.copy(type = MottakerType.KOPI), bruker)
                }
            }
    }

    fun oppdaterTittel(
        id: BrevID,
        tittel: String,
        bruker: BrukerTokenInfo,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterTittel(id, tittel, bruker)
            .also { logger.info("Tittel på brev (id=$id) oppdatert") }
    }

    fun oppdaterSpraak(
        id: BrevID,
        spraak: Spraak,
        bruker: BrukerTokenInfo,
    ) {
        sjekkOmBrevKanEndres(id)

        db
            .oppdaterSpraak(id, spraak, bruker)
            .also { logger.info("Språk i brev (id=$id) endret til $spraak") }
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf =
        pdfGenerator.genererPdf(
            id,
            bruker,
            avsenderRequest = { b, vedtak, enhet -> opprettAvsenderRequest(b, vedtak, enhet) },
            brevKodeMapping = { Brevkoder.TOMT_INFORMASJONSBREV },
            brevDataMapping = { ManueltBrevMedTittelData(it.innholdMedVedlegg.innhold(), it.tittel) },
        )

    suspend fun ferdigstill(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ) {
        val brev = sjekkOmBrevKanEndres(id)

        if (brev.mottakere.size !in 1..2) {
            logger.error("Brev ${brev.id} har ${brev.mottakere.size} mottakere. Dette skal ikke være mulig...")
            throw UgyldigAntallMottakere()
        } else if (brev.mottakere.any { it.erGyldig().isNotEmpty() }) {
            sikkerlogger.error("Ugyldig mottaker: ${brev.mottakere.toJson()}")
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id, brev.sakId, brev.mottakere.flatMap { it.erGyldig() })
        } else if (brev.prosessType == BrevProsessType.OPPLASTET_PDF) {
            db.settBrevFerdigstilt(id, bruker)
        } else {
            val pdf = genererPdf(id, bruker)
            db.lagrePdfOgFerdigstillBrev(id, pdf, bruker)
        }
    }

    suspend fun journalfoer(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ) = journalfoerBrevService.journalfoer(id, bruker)

    fun slett(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ) {
        logger.info("Sjekker om brev med id=$id kan slettes")

        val brev = sjekkOmBrevKanEndres(id)
        check(brev.behandlingId == null) { "Brev med id=$id er et vedtaksbrev og kan ikke slettes" }

        val result = db.settBrevSlettet(id, bruker)
        logger.info("Brev med id=$id slettet=$result")
    }

    fun markerSomUtgaatt(
        id: BrevID,
        kommentar: String,
        bruker: BrukerTokenInfo,
    ) {
        val brev = db.hentBrev(id)

        if (brev.status !in listOf(Status.FERDIGSTILT, Status.JOURNALFOERT)) {
            throw UgyldigForespoerselException(
                "KAN_IKKE_MARKERE_SOM_UTGAATT",
                "Brev har status ${brev.status} og kan ikke markeres som utgått. " +
                    "Det er kun brev med status FERDIGSTILT eller JOURNALFOERT som kan markeres som utgått",
            )
        }

        val alderIDager = Duration.between(brev.opprettet, Tidspunkt.now()).toDays()
        if (alderIDager < 7) {
            throw UgyldigForespoerselException(
                "KAN_IKKE_MARKERE_SOM_UTGAATT",
                "Brevet er kun $alderIDager dag(er) gammelt. Må være minst en uke gammelt for å markeres som utgått",
            )
        }

        db.settBrevUtgaatt(id, kommentar, bruker)
    }

    private fun sjekkOmBrevKanEndres(brevID: BrevID): Brev {
        val brev = db.hentBrev(brevID)

        return if (brev.kanEndres()) {
            brev
        } else {
            throw BrevKanIkkeEndres(brev)
        }
    }
}

class BrevKanIkkeEndres(
    brev: Brev,
) : UgyldigForespoerselException(
        code = "BREV_KAN_IKKE_ENDRES",
        detail = "Brevet kan ikke endres siden det har status ${brev.status.name.lowercase()}",
        meta =
            mapOf(
                "brevId" to brev.id,
                "status" to brev.status,
            ),
    )

class MaksAntallMottakere : UgyldigForespoerselException("MAKS_ANTALL_MOTTAKERE", "Maks 2 mottakere tillatt")

class KanIkkeSletteHovedmottaker :
    UgyldigForespoerselException(
        code = "KAN_IKKE_SLETTE_HOVEDMOTTAKER",
        detail = "Kan ikke slette hovedmottakeren på et brev",
    )

class MinstEnMottakerPaakrevd :
    UgyldigForespoerselException(
        code = "MINST_EN_MOTTAKER_PAAKREVD",
        detail = "Kan ikke slette mottaker. Det må finnes minst 1 mottaker på brevet!",
    )
