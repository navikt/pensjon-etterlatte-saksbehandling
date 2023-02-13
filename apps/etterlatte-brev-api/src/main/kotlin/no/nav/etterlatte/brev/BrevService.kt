package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.AnnetBrevRequest
import no.nav.etterlatte.brev.model.Attestant
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevID
import no.nav.etterlatte.libs.common.brev.model.BrevInnhold
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import org.slf4j.LoggerFactory
import java.util.UUID
import no.nav.etterlatte.brev.model.Mottaker as BrevMottaker

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val adresseService: AdresseService
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentAlleBrev(behandlingId: UUID): List<Brev> = db.hentBrevForBehandling(behandlingId)

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

        val avsender = adresseService.hentAvsenderEnhet(enhet, "")

        val request = AnnetBrevRequest(mal, Spraak.NB, avsender, brevMottaker, Attestant("", ""))

        return BrevInnhold(mal.tittel, Spraak.NB, pdfGenerator.genererPdf(request))
    }

    fun lagreAnnetBrev(behandlingId: UUID, mottaker: Mottaker, brevInnhold: BrevInnhold): Brev {
        return db.opprettBrev(UlagretBrev(behandlingId, brevInnhold.mal, Spraak.NB, mottaker, false, brevInnhold.data))
    }

    fun ferdigstillBrev(id: BrevID): Boolean {
        logger.info("Ferdigstiller brev (id=$id)")

        val brev = db.hentBrev(id)
        if (brev.status == Status.FERDIGSTILT) {
            logger.warn("Brev med id=$id er allerede markert som ferdigstilt. Avbryter ferdigstilling!")
            return true
        }

        return db.oppdaterStatus(id, Status.FERDIGSTILT)
            .also {
                if (it) {
                    logger.info("Brev status er satt til ${Status.FERDIGSTILT}")
                } else {
                    logger.error("Kunne ikke sette brev status til ${Status.FERDIGSTILT}")
                }
            }
    }
}