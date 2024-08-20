package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class BrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val pdfGenerator: PDFGenerator,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentBrev(id: BrevID): Brev = db.hentBrev(id)

    fun hentBrevForSak(sakId: Long): List<Brev> = db.hentBrevForSak(sakId)

    suspend fun opprettNyttManueltBrev(
        sakId: Long,
        bruker: BrukerTokenInfo,
        brevkode: Brevkoder,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
    ): Brev =
        brevoppretter
            .opprettBrev(
                sakId = sakId,
                behandlingId = null,
                bruker = bruker,
                brevKodeMapping = { brevkode },
                brevtype = brevkode.ferdigstilling.brevtype,
                brevDataMapping = brevDataMapping,
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

        if (!brev.mottaker.erGyldig()) {
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id)
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
