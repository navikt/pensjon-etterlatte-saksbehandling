package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevRequestMapper
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.UlagretBrev
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
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

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId).find { it.erVedtaksbrev }
    }

    suspend fun oppdaterVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): Brev {
        val vedtaksbrev = hentVedtaksbrev(behandlingId)

        if (vedtaksbrev?.status != null && vedtaksbrev.status !in listOf(Status.OPPRETTET, Status.OPPDATERT)) {
            logger.info(
                "Vedtaksbrev har status ${vedtaksbrev.status} og kan ikke endres. Returnerer lagret brev for visning."
            )

            return vedtaksbrev
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)
        val nyttBrev = opprettEllerOppdater(behandling)

        return if (vedtaksbrev == null) {
            db.opprettBrev(nyttBrev)
        } else {
            db.oppdaterBrev(vedtaksbrev.id, nyttBrev)
            vedtaksbrev
        }
    }

    fun ferdigstillVedtaksbrev(behandlingId: UUID): Boolean {
        logger.info("Ferdigstiller vedtaksbrev for behandlingId=$behandlingId")

        val vedtaksbrev = hentVedtaksbrev(behandlingId)
            ?: throw IllegalArgumentException("Vedtaksbrev ikke funnet. Avbryter ferdigstilling/attestering.")

        return when (vedtaksbrev.status) {
            in listOf(Status.OPPRETTET, Status.OPPDATERT) -> db.settBrevFerdigstilt(vedtaksbrev.id)
            Status.FERDIGSTILT -> true // Ignorer hvis brev allerede er ferdigstilt
            else -> throw IllegalArgumentException(
                "Kan ikke ferdigstille vedtaksbrev (id=${vedtaksbrev.id}) med status ${vedtaksbrev.status}"
            )
        }
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

    private suspend fun opprettEllerOppdater(behandling: Behandling): UlagretBrev {
        val (avsender, attestant) = adresseService.hentAvsenderOgAttestant(behandling.vedtak)

        val mottaker = adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)

        val vedtakType = behandling.vedtak.type

        val brevRequest = BrevRequestMapper.fra(vedtakType, behandling, avsender, mottaker, attestant)

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${behandling.vedtak.id}) med størrelse: ${pdf.size}")

        return UlagretBrev(
            behandling.behandlingId,
            soekerFnr = behandling.persongalleri.soeker.fnr,
            tittel = "Vedtak om ${vedtakType.name.lowercase()}",
            behandling.spraak,
            mottaker,
            erVedtaksbrev = true,
            pdf
        )
    }
}