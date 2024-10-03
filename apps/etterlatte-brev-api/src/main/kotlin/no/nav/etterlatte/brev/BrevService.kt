package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.oppgave.OppgaveService
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.vedtaksbrev.UgyldigMottakerKanIkkeFerdigstilles
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.sakId
import org.slf4j.LoggerFactory

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
    ): BrevID {
        val (brev, enhetsnummer) =
            brevoppretter.opprettBrevSomHarInnhold(
                sakId = req.sakId,
                behandlingId = null,
                bruker = bruker,
                brevKode = req.brevKode,
                brevData = req.brevDataRedigerbar,
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
            distribuerer.distribuer(brevId)

            logger.info("Brevid: $brevId er distribuert")

            return brevId
        } catch (e: Exception) {
            logger.error("Feil opp sto under ferdigstill/journalfør/distribuer av brevID=${brev.id}...", e)
            oppgaveService.opprettOppgaveForFeiletBrev(req.sakId, brevId, bruker)
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
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterPayload(id, payload)
            .also { logger.info("Payload for brev (id=$id) oppdatert") }
    }

    fun lagreBrevPayloadVedlegg(
        id: BrevID,
        payload: List<BrevInnholdVedlegg>,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterPayloadVedlegg(id, payload)
            .also { logger.info("Vedlegg payload for brev (id=$id) oppdatert") }
    }

    fun oppdaterMottaker(
        id: BrevID,
        mottaker: Mottaker,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterMottaker(id, mottaker)
            .also { logger.info("Mottaker på brev (id=$id) oppdatert") }
    }

    fun oppdaterTittel(
        id: BrevID,
        tittel: String,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db
            .oppdaterTittel(id, tittel)
            .also { logger.info("Tittel på brev (id=$id) oppdatert") }
    }

    fun oppdaterSpraak(
        id: BrevID,
        spraak: Spraak,
    ) {
        sjekkOmBrevKanEndres(id)

        db
            .oppdaterSpraak(id, spraak)
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

        if (brev.mottaker.erGyldig().isNotEmpty()) {
            sikkerlogger.error("Ugyldig mottaker: ${brev.mottaker.toJson()}")
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id, brev.sakId, brev.mottaker.erGyldig())
        } else if (brev.prosessType == BrevProsessType.OPPLASTET_PDF) {
            db.settBrevFerdigstilt(id)
        } else {
            val pdf = genererPdf(id, bruker)
            db.lagrePdfOgFerdigstillBrev(id, pdf)
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
