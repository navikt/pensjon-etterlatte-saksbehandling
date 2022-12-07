package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.model.AnnetBrevRequest
import no.nav.etterlatte.brev.model.AvslagBrevRequest
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.BrevID
import no.nav.etterlatte.libs.common.brev.model.BrevInnhold
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.util.UUID
import no.nav.etterlatte.brev.model.Mottaker as BrevMottaker

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val sendToRapid: (String) -> Unit
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentAlleBrev(behandlingId: String): List<Brev> = db.hentBrevForBehandling(behandlingId)

    fun hentBrevInnhold(id: BrevID): BrevInnhold = db.hentBrevInnhold(id)

    fun slettBrev(id: BrevID): Boolean {
        val brev = db.hentBrev(id)

        if (brev.status != Status.OPPRETTET && brev.status != Status.OPPDATERT) {
            throw RuntimeException("Brev er ferdigstilt og kan ikke slettes!")
        } else if (brev.erVedtaksbrev) {
            throw RuntimeException("Vedtaksbrev kan ikke slettes!")
        }

        return db.slett(id)
    }

    suspend fun opprett(mottaker: Mottaker, mal: Mal, enhet: String): BrevInnhold {
        val brevMottaker = when {
            mottaker.foedselsnummer != null -> adresseService.hentMottakerAdresse(mottaker.foedselsnummer!!.value)
            mottaker.orgnummer != null -> adresseService.hentMottakerAdresse(mottaker.orgnummer!!)
            mottaker.adresse != null -> BrevMottaker.fraAdresse(adresse = mottaker.adresse!!)
            else -> throw Exception("Ingen brevmottaker spesifisert")
        }

        val avsender = adresseService.hentAvsenderEnhet(enhet)

        val request = AnnetBrevRequest(mal, Spraak.NB, avsender, brevMottaker)

        return BrevInnhold(mal.tittel, Spraak.NB.toString(), pdfGenerator.genererPdf(request))
    }

    fun lagreAnnetBrev(behandlingId: String, mottaker: Mottaker, brevInnhold: BrevInnhold): Brev {
        return db.opprettBrev(UlagretBrev(behandlingId, brevInnhold.mal, mottaker, false, brevInnhold.data))
    }

    suspend fun oppdaterVedtaksbrev(sakId: Long, behandlingId: String, accessToken: String = ""): BrevID {
        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, accessToken)
        val nyttBrev = opprettNyttBrevFraVedtak(behandling)

        val vedtaksbrev = db.hentBrevForBehandling(behandlingId)
            .find { it.erVedtaksbrev }

        return if (vedtaksbrev == null) {
            db.opprettBrev(nyttBrev).id
        } else {
            db.oppdaterBrev(vedtaksbrev.id, nyttBrev)
            vedtaksbrev.id
        }
    }

    fun ferdigstillAttestertVedtak(vedtak: Vedtak) = db.hentBrevForBehandling(vedtak.behandling.id.toString())
        .find { it.erVedtaksbrev }
        .let { brev ->
            require(brev != null) {
                "Klarte ikke finne vedtaksbrev for attestert vedtak med behandlingsId '${vedtak.behandling.id}'"
            }
            ferdigstill(brev, vedtak)
        }

    suspend fun ferdigstillBrev(id: BrevID): Brev {
        val brev: Brev = db.hentBrev(id)

        if (brev.erVedtaksbrev) {
            throw RuntimeException("Vedtaksbrev skal ikke ferdigstilles manuelt!")
        }

        return ferdigstill(brev, TODO())
    }

    private fun ferdigstill(brev: Brev, vedtak: Vedtak): Brev {
        sendToRapid(opprettDistribusjonsmelding(brev, vedtak))
        db.oppdaterStatus(brev.id, Status.FERDIGSTILT)

        return brev
    }

    private suspend fun opprettNyttBrevFraVedtak(behandling: Behandling): UlagretBrev {
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

    private fun opprettDistribusjonsmelding(brev: Brev, vedtak: Vedtak): String =
        DistribusjonMelding(
            behandlingId = brev.behandlingId,
            distribusjonType = if (brev.erVedtaksbrev) DistribusjonsType.VEDTAK else DistribusjonsType.VIKTIG,
            brevId = brev.id,
            mottaker = brev.mottaker,
            bruker = Bruker(vedtak.sak.ident),
            tittel = brev.tittel,
            brevKode = "XX.YY-ZZ",
            journalfoerendeEnhet = vedtak.vedtakFattet!!.ansvarligEnhet
        ).let {
            val correlationId = UUID.randomUUID().toString()
            logger.info("Oppretter distribusjonsmelding for brev (id=${brev.id}) med correlation_id=$correlationId")

            JsonMessage.newMessage(
                mapOf(
                    eventNameKey to BrevEventTypes.FERDIGSTILT.toString(),
                    "brevId" to it.brevId,
                    correlationIdKey to correlationId,
                    "payload" to it.toJson()
                )
            ).toJson()
        }

}
