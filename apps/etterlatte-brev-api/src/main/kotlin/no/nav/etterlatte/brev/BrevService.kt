package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_DELMAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class BrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val pdfGenerator: PDFGenerator,
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentBrev(id: BrevID): Brev {
        return db.hentBrev(id)
    }

    fun hentBrevForSak(sakId: Long): List<Brev> {
        return db.hentBrevForSak(sakId)
    }

    suspend fun opprettBrev(
        sakId: Long,
        bruker: BrukerTokenInfo,
    ): Brev =
        brevoppretter.opprettBrev(
            sakId = sakId,
            behandlingId = null,
            bruker = bruker,
            automatiskMigreringRequest = null,
        ).first

    data class BrevPayload(
        val hoveddel: Slate?,
        val vedlegg: List<BrevInnholdVedlegg>?,
    )

    fun hentBrevPayload(id: BrevID): BrevPayload {
        val hoveddel =
            db.hentBrevPayload(id)
                .also { logger.info("Hentet payload for brev (id=$id)") }

        val vedlegg =
            db.hentBrevPayloadVedlegg(id)
                .also { logger.info("Hentet payload til vedlegg for brev (id=$id)") }

        return BrevPayload(hoveddel, vedlegg)
    }

    fun lagreBrevPayload(
        id: BrevID,
        payload: Slate,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterPayload(id, payload)
            .also { logger.info("Payload for brev (id=$id) oppdatert") }
    }

    fun lagreBrevPayloadVedlegg(
        id: BrevID,
        payload: List<BrevInnholdVedlegg>,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterPayloadVedlegg(id, payload)
            .also { logger.info("Vedlegg payload for brev (id=$id) oppdatert") }
    }

    fun oppdaterMottaker(
        id: BrevID,
        mottaker: Mottaker,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterMottaker(id, mottaker)
            .also { logger.info("Mottaker på brev (id=$id) oppdatert") }
    }

    fun oppdaterTittel(
        id: BrevID,
        tittel: String,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterTittel(id, tittel)
            .also { logger.info("Tittel på brev (id=$id) oppdatert") }
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf =
        pdfGenerator.genererPdf(
            id,
            bruker,
            null,
            avsenderRequest = { b, g -> g.avsenderRequest(b) },
            brevKode = { _, _ -> Brevkoder(TOM_DELMAL, TOM_MAL_INFORMASJONSBREV) },
        )

    suspend fun ferdigstill(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ) {
        val brev = sjekkOmBrevKanEndres(id)

        if (brev.prosessType == BrevProsessType.OPPLASTET_PDF) {
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

class BrevKanIkkeEndres(brev: Brev) : UgyldigForespoerselException(
    code = "BREV_KAN_IKKE_ENDRES",
    detail = "Brevet kan ikke endres siden det har status ${brev.status.name.lowercase()}",
    meta =
        mapOf(
            "brevId" to brev.id,
            "status" to brev.status,
        ),
)
