package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PDFGenerator(
    private val db: BrevRepository,
    private val brevDataFacade: BrevdataFacade,
    private val adresseService: AdresseService,
    private val brevbakerService: BrevbakerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val brev = db.hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id))
        }

        val generellBrevData =
            retryOgPakkUt { brevDataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId, bruker) }

        val sak = generellBrevData.sak
        val avsender =
            adresseService.hentAvsender(AvsenderRequest(saksbehandlerIdent = bruker.ident(), sakenhet = sak.enhet))

        val (brevKode, brevData) = opprettBrevData(brev) // TODO samme mekanisme som elles?
        val brevRequest =
            BrevbakerRequest.fra(
                brevKode = brevKode,
                letterData = brevData,
                avsender = avsender,
                soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                sakId = sak.id,
                spraak = generellBrevData.spraak,
                sakType = sak.sakType,
            )

        return brevbakerService.genererPdf(brev.id, brevRequest)
    }

    private fun opprettBrevData(brev: Brev): Pair<EtterlatteBrevKode, BrevData> {
        val payload = requireNotNull(db.hentBrevPayload(brev.id))

        return Pair(EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV, ManueltBrevMedTittelData(payload.elements, brev.tittel))
    }
}
