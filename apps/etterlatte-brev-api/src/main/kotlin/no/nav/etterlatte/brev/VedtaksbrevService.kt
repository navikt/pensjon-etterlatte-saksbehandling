package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.AvslagBrevRequest
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.journalpost.DokarkivServiceImpl
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevID
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory

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
        behandlingId: String,
        saksbehandler: String,
        accessToken: String = ""
    ): BrevID {
        val vedtaksbrev = db.hentBrevForBehandling(behandlingId)
            .find { it.erVedtaksbrev }

        if (vedtaksbrev?.status != null && vedtaksbrev.status !in listOf(Status.OPPRETTET, Status.OPPDATERT)) {
            throw IllegalArgumentException("Vedtaksbrev status er ${vedtaksbrev.status}. Kan derfor ikke endres.")
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, saksbehandler, accessToken)
        val nyttBrev = opprettEllerOppdater(behandling)

        return if (vedtaksbrev == null) {
            db.opprettBrev(nyttBrev).id
        } else {
            db.oppdaterBrev(vedtaksbrev.id, nyttBrev)
            vedtaksbrev.id
        }
    }

    fun journalfoerVedtaksbrev(vedtak: Vedtak): Pair<Brev, JournalpostResponse> {
        val vedtaksbrev = db.hentBrevForBehandling(vedtak.behandling.id.toString())
            .single { it.erVedtaksbrev }

        if (vedtaksbrev.status != Status.FERDIGSTILT) {
            throw IllegalArgumentException("Ugyldig status ${vedtaksbrev.status} på vedtaksbrev (id=${vedtaksbrev.id})")
        }

        val melding = DistribusjonMelding(
            behandlingId = vedtaksbrev.behandlingId,
            distribusjonType = if (vedtaksbrev.erVedtaksbrev) DistribusjonsType.VEDTAK else DistribusjonsType.VIKTIG,
            brevId = vedtaksbrev.id,
            mottaker = vedtaksbrev.mottaker,
            bruker = Bruker(vedtak.sak.ident),
            tittel = vedtaksbrev.tittel,
            brevKode = "XX.YY-ZZ",
            journalfoerendeEnhet = vedtak.vedtakFattet!!.ansvarligEnhet
        )

        val response = dokarkivService.journalfoer(melding)

        db.oppdaterStatus(vedtaksbrev.id, Status.JOURNALFOERT, response.toJson())
            .also { logger.info("Brev med id=${vedtaksbrev.id} markert som ferdigstilt") }

        return vedtaksbrev to response
    }

    private suspend fun opprettEllerOppdater(behandling: Behandling): UlagretBrev {
        val enhet = behandling.vedtak.enhet
        val avsender = adresseService.hentAvsenderEnhet(enhet)
        val mottaker = adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)

        val vedtakType = behandling.vedtak.type

        val brevRequest = when (vedtakType) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(behandling, avsender, mottaker)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(behandling, avsender, mottaker)
            else -> throw Exception("Vedtakstype er ikke støttet: $vedtakType")
        }

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${behandling.vedtak.id}) med størrelse: ${pdf.size}")

        val tittel = "Vedtak om ${vedtakType.name.lowercase()}"

        return UlagretBrev(
            behandling.behandlingId,
            tittel,
            Mottaker(
                foedselsnummer = Foedselsnummer.of(behandling.persongalleri.innsender.fnr),
                adresse = Adresse(
                    behandling.persongalleri.innsender.navn
                    // TODO: skrive om objekter
                )
            ),
            true,
            pdf
        )
    }
}