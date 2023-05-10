package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevRequestMapper
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class VedtaksbrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val dokarkivService: DokarkivServiceImpl
) {
    private val logger = LoggerFactory.getLogger(VedtaksbrevService::class.java)

    fun hentBrev(id: BrevID): Brev {
        logger.info("Henter brev (id=$id)")

        return db.hentBrev(id)
    }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId)?.takeIf { it.erVedtaksbrev }
    }

    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): Brev {
        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)

        val mottaker = adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)

        val vedtakType = behandling.vedtak.type

        val nyttBrev = OpprettNyttBrev(
            behandlingId = behandling.behandlingId,
            soekerFnr = behandling.persongalleri.soeker.fnr,
            tittel = "Vedtak om ${vedtakType.name.lowercase()}",
            mottaker = mottaker,
            erVedtaksbrev = true
        )

        // TODO: Dette er en midlertidig hack for å forhindre dobbel insert ved lokal kjøring.
        //       Vil bli fikset i kommende commit som tar for seg en større endring i databasen.
        require(hentVedtaksbrev(behandlingId) == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        return db.opprettBrev(nyttBrev)
    }

    suspend fun genererPdfInnhold(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): ByteArray {
        val brev = requireNotNull(hentVedtaksbrev(behandlingId))

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentBrevInnhold(brev.id)).data
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)

        val pdf = genererPdf(behandling, brev.mottaker)

        if (behandling.vedtak.status == VedtakStatus.FATTET_VEDTAK) {
            logger.info("Behandling har fått status ${VedtakStatus.FATTET_VEDTAK} – låser brevets innhold")
            if (behandling.vedtak.saksbehandler.ident != bruker.ident()) {
                db.opprettInnholdOgFerdigstill(brev.id, BrevInnhold(behandling.spraak, pdf))
            }
        }

        return pdf
    }

    fun journalfoerVedtaksbrev(vedtaksbrev: Brev, vedtak: VedtakTilJournalfoering): Pair<Brev, JournalpostResponse> {
        if (vedtaksbrev.status != Status.FERDIGSTILT) {
            throw IllegalArgumentException("Ugyldig status ${vedtaksbrev.status} på vedtaksbrev (id=${vedtaksbrev.id})")
        }

        val response = dokarkivService.journalfoer(vedtaksbrev, vedtak)

        db.settBrevJournalfoert(vedtaksbrev.id, response)
            .also { logger.info("Brev med id=${vedtaksbrev.id} markert som journalført") }

        return vedtaksbrev to response
    }

    fun slettVedtaksbrev(id: BrevID): Boolean {
        logger.info("Sletter vedtaksbrev (id=$id)")

        return db.slett(id)
    }

    private suspend fun genererPdf(behandling: Behandling, mottaker: Mottaker): ByteArray {
        val (avsender, attestant) = adresseService.hentAvsenderOgAttestant(behandling.vedtak)

        val brevRequest = BrevRequestMapper.fra(behandling, avsender, mottaker, attestant)

        return pdfGenerator.genererPdf(brevRequest)
    }
}