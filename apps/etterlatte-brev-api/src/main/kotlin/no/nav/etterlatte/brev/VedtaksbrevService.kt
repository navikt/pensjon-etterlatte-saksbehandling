package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.model.AvslagBrevRequest
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevID
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import org.slf4j.LoggerFactory

class VedtaksbrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val distribusjonService: DistribusjonService
) {
    private val logger = LoggerFactory.getLogger(VedtaksbrevService::class.java)

    suspend fun oppdaterVedtaksbrev(sakId: Long, behandlingId: String, accessToken: String = ""): BrevID {
        if (tilAttestering())
            throw Exception("Vedtaksbrev er allerede sendt til attestering. Kan derfor ikke endres.")

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, accessToken)
        val nyttBrev = opprettEllerOppdater(behandling)

        val vedtaksbrev = db.hentBrevForBehandling(behandlingId)
            .find { it.erVedtaksbrev }

        return if (vedtaksbrev == null) {
            db.opprettBrev(nyttBrev).id
        } else {
            db.oppdaterBrev(vedtaksbrev.id, nyttBrev)
            vedtaksbrev.id
        }
    }

    fun sendTilDistribusjon(vedtak: Vedtak): BrevID {
        val vedtaksbrev = db.hentBrevForBehandling(vedtak.behandling.id.toString())
            .find { it.erVedtaksbrev }

        requireNotNull(vedtaksbrev) {
            "Klarte ikke finne vedtaksbrev for attestert vedtak med behandlingsId '${vedtak.behandling.id}'"
        }

        return ferdigstill(vedtaksbrev, vedtak)
    }

    private fun tilAttestering(): Boolean {
        // TODO: Legge til status for sak til ATTESTERING

        return false
    }

    private fun ferdigstill(brev: Brev, vedtak: Vedtak): BrevID {
        distribusjonService.distribuerBrev(brev, vedtak)

        db.oppdaterStatus(brev.id, Status.FERDIGSTILT)
            .also { logger.info("Brev med id=${brev.id} markert som ferdigstilt") }

        return brev.id
    }

    private suspend fun opprettEllerOppdater(behandling: Behandling): UlagretBrev {
        val (_, behandlingId, persongalleri, vedtak) = behandling

        val enhet = vedtak.vedtakFattet?.ansvarligEnhet
            ?: "0805" // TODO: Midlertidig Porsgrunn inntil vedtak inneholder gyldig enhet
        val avsender = adresseService.hentAvsenderEnhet(enhet)
        val mottaker = adresseService.hentMottakerAdresse(persongalleri.innsender.fnr)

        val brevRequest = when (vedtak.type) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(behandling, avsender, mottaker)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(behandling, avsender, mottaker)
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${pdf.size}")

        val tittel = "Vedtak om ${vedtak.type.name.lowercase()}"

        return UlagretBrev(
            behandlingId,
            tittel,
            Mottaker(
                foedselsnummer = Foedselsnummer.of(persongalleri.innsender.fnr),
                adresse = Adresse(
                    persongalleri.innsender.navn
                    // TODO: skrive om objekter
                )
            ),
            true,
            pdf
        )
    }

}
