package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.AvslagBrevRequest
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.UlagretBrev
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val dokarkivService: DokarkivServiceImpl
) {
    private val logger = LoggerFactory.getLogger(VedtaksbrevService::class.java)

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
            in listOf(Status.OPPRETTET, Status.OPPDATERT) -> db.oppdaterStatus(vedtaksbrev.id, Status.FERDIGSTILT)
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

        db.setJournalpostId(vedtaksbrev.id, response.journalpostId)
        db.oppdaterStatus(vedtaksbrev.id, Status.JOURNALFOERT, response.toJson())
            .also { logger.info("Brev med id=${vedtaksbrev.id} markert som ferdigstilt") }

        return vedtaksbrev to response
    }

    private suspend fun opprettEllerOppdater(behandling: Behandling): UlagretBrev {
        val (avsender, attestant) = adresseService.hentAvsenderOgAttestant(behandling.vedtak)

        val mottaker = adresseService.hentMottakerAdresse(behandling.persongalleri.soeker.fnr)

        val vedtakType = behandling.vedtak.type

        val brevRequest = when (vedtakType) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(behandling, avsender, mottaker, attestant)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(behandling, avsender, mottaker, attestant)
            else -> throw Exception("Vedtakstype er ikke støttet: $vedtakType")
        }

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${behandling.vedtak.id}) med størrelse: ${pdf.size}")

        return UlagretBrev(
            behandling.behandlingId,
            soekerFnr = behandling.persongalleri.soeker.fnr,
            tittel = "Vedtak om ${vedtakType.name.lowercase()}",
            behandling.spraak,
            Mottaker(
                foedselsnummer = Foedselsnummer.of(behandling.persongalleri.soeker.fnr),
                adresse = Adresse(
                    navn = behandling.persongalleri.soeker.navn,
                    adresse = mottaker.adresse,
                    postnummer = mottaker.postnummer,
                    poststed = mottaker.poststed,
                    land = mottaker.land
                )
            ),
            erVedtaksbrev = true,
            pdf
        )
    }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? =
        db.hentBrevForBehandling(behandlingId).find { it.erVedtaksbrev }
}