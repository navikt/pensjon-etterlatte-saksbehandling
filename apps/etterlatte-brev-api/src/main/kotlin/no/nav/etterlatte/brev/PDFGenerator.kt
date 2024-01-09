package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataMapper.BrevkodePar
import no.nav.etterlatte.brev.model.BrevID
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
        automatiskMigreringRequest: MigreringBrevRequest?,
        avsenderRequest: (GenerellBrevData) -> AvsenderRequest,
        brevKode: (GenerellBrevData, Brev, MigreringBrevRequest?) -> BrevkodePar,
        brevData: (BrevDataRequest) -> BrevData,
    ): Pair<Pdf, BrevData?> {
        val brev = db.hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return Pair(requireNotNull(db.hentPdf(brev.id)) { "Fant ikke brev med id ${brev.id}" }, null)
        }

        val generellBrevData =
            retryOgPakkUt { brevDataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId, bruker) }
        val avsender = adresseService.hentAvsender(avsenderRequest(generellBrevData))

        val brevkodePar = brevKode(generellBrevData, brev, automatiskMigreringRequest)

        val sak = generellBrevData.sak
        val letterData =
            brevData(
                BrevDataRequest(
                    generellBrevData,
                    automatiskMigreringRequest,
                    brev,
                    bruker,
                    brevkodePar,
                ),
            )
        val brevRequest =
            BrevbakerRequest.fra(
                brevKode = brevkodePar.ferdigstilling,
                letterData = letterData,
                avsender = avsender,
                soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                sakId = sak.id,
                spraak = generellBrevData.spraak,
                sakType = sak.sakType,
            )

        return Pair(brevbakerService.genererPdf(brev.id, brevRequest), letterData)
    }
}

data class BrevDataRequest(
    val generellBrevData: GenerellBrevData,
    val automatiskMigreringRequest: MigreringBrevRequest? = null,
    val brev: Brev,
    val bruker: BrukerTokenInfo,
    val brevkodePar: BrevkodePar,
)
