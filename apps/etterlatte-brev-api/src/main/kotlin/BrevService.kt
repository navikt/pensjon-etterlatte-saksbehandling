package no.nav.etterlatte

import model.brev.AnnetBrevRequest
import model.brev.AvslagBrevRequest
import model.brev.InnvilgetBrevRequest
import no.nav.etterlatte.db.*
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.vedtak.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import pdf.PdfGeneratorKlient
import java.util.*
import no.nav.etterlatte.model.brev.Mottaker as BrevMottaker

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val vedtakService: VedtakService,
    private val sendToRapid: (String) -> Unit
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    suspend fun hentAlleBrev(behandlingId: String): List<Brev> {
        val brev = db.hentBrevForBehandling(behandlingId.toLong())

        return brev.ifEmpty {
            logger.info("Ingen brev funnet for behandling $behandlingId. Forsøker å opprette fra vedtak.")
            listOf(opprettFraVedtak(behandlingId))
        }
    }

    fun hentBrevInnhold(id: BrevID): BrevInnhold = db.hentBrevInnhold(id)

    fun slettBrev(id: BrevID): Boolean {
        val brev = db.hentBrev(id)

        if (brev.status != Status.OPPRETTET && brev.status != Status.OPPDATERT)
            throw RuntimeException("Brev er ferdigstilt og kan ikke slettes!")

        return db.slett(id)
    }

    suspend fun opprett(behandlingId: String, mottaker: Mottaker, mal: Mal): Brev {
        val brevMottaker = BrevMottaker(
            fornavn = mottaker.fornavn,
            etternavn = mottaker.etternavn,
            adresse = mottaker.adresse.adresse,
            postnummer = mottaker.adresse.postnummer,
            poststed = mottaker.adresse.poststed,
            land = mottaker.adresse.land
        )

        val request = AnnetBrevRequest(mal, Spraak.NB, brevMottaker)

        val pdf = pdfGenerator.genererPdf(request)

        return db.opprettBrev(NyttBrev(behandlingId.toLong(), mal.tittel, mottaker, pdf))
    }

    fun ferdigstillBrev(id: BrevID): Brev {
        val brev: Brev = db.hentBrev(id)

        sendToRapid(opprettDistribusjonsmelding(brev))
        db.oppdaterStatus(id, Status.FERDIGSTILT)

        return brev
    }

    private suspend fun opprettFraVedtak(behandlingId: String): Brev {
        val vedtak = vedtakService.hentVedtak(behandlingId.toLong())

        val brevRequest = when (vedtak.type) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(vedtak)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(vedtak)
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${pdf.size}")

        val tittel = "Vedtak om ${vedtak.type.name.lowercase()}"
        val mottaker = Mottaker(
            fornavn = brevRequest.mottaker.fornavn,
            etternavn = brevRequest.mottaker.etternavn,
            adresse = Adresse(
                adresse = brevRequest.mottaker.adresse,
                postnummer = brevRequest.mottaker.postnummer,
                poststed = brevRequest.mottaker.poststed,
                land = brevRequest.mottaker.land
            ),
        )
        return db.opprettBrev(NyttBrev(behandlingId.toLong(), tittel, mottaker, pdf))
    }

    private fun opprettDistribusjonsmelding(brev: Brev): String = DistribusjonMelding(
        vedtakId = "Vedtak_Id",
        brevId = brev.id,
        mottaker = AvsenderMottaker(brev.mottaker.foedselsnummer?.value ?: "0101202212345"),
        bruker = Bruker(brev.mottaker.foedselsnummer?.value ?: "0101202212345"),
        tittel = brev.tittel
    ).let {
        val correlationId = UUID.randomUUID().toString()
        logger.info("Oppretter distribusjonsmelding for brev (id=${brev.id}) med correlation_id=$correlationId")

        JsonMessage.newMessage(
            mapOf(
                "@event" to "BREV:DISTRIBUER",
                "@brevId" to it.brevId,
                "@correlation_id" to correlationId,
                "payload" to it.toJson()
            )
        ).toJson()
    }
}
