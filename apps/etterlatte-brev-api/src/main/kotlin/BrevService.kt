package no.nav.etterlatte

import model.brev.AnnetBrevRequest
import model.brev.AvslagBrevRequest
import model.brev.InnvilgetBrevRequest
import model.brev.mapper.finnBarn
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.brev.model.*
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.vedtak.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import pdf.PdfGeneratorKlient
import java.util.UUID
import no.nav.etterlatte.model.brev.Mottaker as BrevMottaker

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val vedtakService: VedtakService,
    private val sendToRapid: (String) -> Unit
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentAlleBrev(behandlingId: String): List<Brev> = db.hentBrevForBehandling(behandlingId)

    fun hentBrevInnhold(id: BrevID): BrevInnhold = db.hentBrevInnhold(id)

    fun slettBrev(id: BrevID): Boolean {
        val brev = db.hentBrev(id)

        if (brev.status != Status.OPPRETTET && brev.status != Status.OPPDATERT)
            throw RuntimeException("Brev er ferdigstilt og kan ikke slettes!")

        return db.slett(id)
    }

    suspend fun opprett(behandlingId: String, mottaker: Mottaker, mal: Mal): Brev {
        val brevMottaker = when {
            // todo: hent adresse fra pdl
            mottaker.foedselsnummer != null -> BrevMottaker("Fornavn", "Fødselsnummer", "Veien 22", "0000", "Oslo")
            // todo: hent adresse fra pdl og skriv om mottaker for brev til å støtte organisasjoner.
            mottaker.orgnummer != null -> BrevMottaker("Fornavn", "Organisasjon", "Veien 22", "0000", "Oslo")
            mottaker.adresse != null -> BrevMottaker.fraAdresse(adresse = mottaker.adresse!!)
            else -> throw Exception("Ingen brevmottaker spesifisert")
        }

        val request = AnnetBrevRequest(mal, Spraak.NB, brevMottaker)

        val pdf = pdfGenerator.genererPdf(request)

        return db.opprettBrev(UlagretBrev(behandlingId, mal.tittel, mottaker, false, pdf))
    }

    suspend fun oppdaterVedtaksbrev(behandlingId: String): BrevID {
        val vedtak = vedtakService.hentVedtak(behandlingId)
        val nyttBrev = opprettNyttBrevFraVedtak(vedtak, behandlingId)

        val vedtaksbrev = db.hentBrevForBehandling(behandlingId)
            .find { it.erVedtaksbrev }

        return if (vedtaksbrev == null)
            db.opprettBrev(nyttBrev).id
        else {
            db.oppdaterBrev(vedtaksbrev.id, nyttBrev)
            vedtaksbrev.id
        }
    }

    // TODO: Undersøke om denne er nødvendig
    suspend fun opprettFraVedtak(vedtak: Vedtak): Brev =
        opprettNyttBrevFraVedtak(vedtak)
            .let { db.opprettBrev(it) }

    fun ferdigstillBrev(id: BrevID): Brev {
        val brev: Brev = db.hentBrev(id)

        // todo: vedtak må byttes ut med grunnlag for å fungere på behandlinger også.
        val vedtak = vedtakService.hentVedtak(brev.behandlingId)

        sendToRapid(opprettDistribusjonsmelding(brev, vedtak))
        db.oppdaterStatus(id, Status.FERDIGSTILT)

        return brev
    }

    private suspend fun opprettNyttBrevFraVedtak(vedtak: Vedtak, behandlingId: String? = null): UlagretBrev {
        val brevRequest = when (vedtak.type) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(vedtak)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(vedtak)
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${pdf.size}")

        val tittel = "Vedtak om ${vedtak.type.name.lowercase()}"
        val mottaker = Mottaker(Foedselsnummer.of(vedtak.finnBarn().fnr))

        return UlagretBrev(
            behandlingId = behandlingId ?: vedtak.behandling.id.toString(),
            tittel,
            mottaker,
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
            bruker = Bruker(vedtak.finnBarn().fnr),
            tittel = brev.tittel,
            brevKode = "XX.YY-ZZ",
            journalfoerendeEnhet = vedtak.vedtakFattet!!.ansvarligEnhet
        ).let {
            val correlationId = UUID.randomUUID().toString()
            logger.info("Oppretter distribusjonsmelding for brev (id=${brev.id}) med correlation_id=$correlationId")

            JsonMessage.newMessage(
                mapOf(
                    "@event" to BrevEventTypes.FERDIGSTILT.toString(),
                    "@brevId" to it.brevId,
                    "@correlation_id" to correlationId,
                    "payload" to it.toJson()
                )
            ).toJson()
        }
}
