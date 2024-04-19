package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PDFGenerator(
    private val db: BrevRepository,
    private val brevDataFacade: BrevdataFacade,
    private val adresseService: AdresseService,
    private val brevbakerService: BrevbakerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ferdigstillOgGenererPDF(
        id: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, GenerellBrevData) -> AvsenderRequest,
        brevKode: (BrevkodeRequest) -> Brevkoder,
        brevData: suspend (BrevDataFerdigstillingRequest) -> BrevDataFerdigstilling,
        lagrePdfHvisVedtakFattet: (GenerellBrevData, Brev, Pdf) -> Unit = { _, _, _ -> run {} },
    ): Pdf {
        val brev = sjekkOmBrevKanEndres(id)
        if (!brev.mottaker.erGyldig()) {
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id)
        }

        val pdf =
            genererPdf(
                id,
                bruker,
                avsenderRequest,
                brevKode,
                brevData,
                lagrePdfHvisVedtakFattet,
            )
        db.lagrePdfOgFerdigstillBrev(id, pdf)
        return pdf
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, GenerellBrevData) -> AvsenderRequest,
        brevKode: (BrevkodeRequest) -> Brevkoder,
        brevData: suspend (BrevDataFerdigstillingRequest) -> BrevDataFerdigstilling,
        lagrePdfHvisVedtakFattet: (GenerellBrevData, Brev, Pdf) -> Unit = { _, _, _ -> run {} },
    ): Pdf {
        val brev = db.hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id)) { "Fant ikke brev med id ${brev.id}" }
        } else if (brev.prosessType == BrevProsessType.OPPLASTET_PDF) {
            logger.info("Brev er en opplastet PDF – returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id)) { "Fant ikke pdf for brev med id=${brev.id}" }
        }
        val behandlingId = brev.behandlingId?.takeIf { brev.brevtype.erKobletTilEnBehandling() }

        val generellBrevData =
            retryOgPakkUt { brevDataFacade.hentGenerellBrevData(brev.sakId, behandlingId, brev.spraak, bruker) }
        val avsender = adresseService.hentAvsender(avsenderRequest(bruker, generellBrevData))

        val brevkodePar =
            brevKode(
                BrevkodeRequest(
                    generellBrevData.loependeIPesys(),
                    generellBrevData.erForeldreloes(),
                    generellBrevData.sak.sakType,
                    generellBrevData.forenkletVedtak?.type,
                ),
            )

        val sak = generellBrevData.sak
        val brevRequest =
            BrevbakerRequest.fra(
                brevKode = brevkodePar.ferdigstilling,
                brevData =
                    brevData(
                        BrevDataFerdigstillingRequest(
                            generellBrevData,
                            bruker,
                            InnholdMedVedlegg({ hentLagretInnhold(brev) }, { hentLagretInnholdVedlegg(brev) }),
                            brevkodePar,
                            brev.tittel,
                        ),
                    ),
                avsender = avsender,
                soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                sakId = sak.id,
                spraak = generellBrevData.spraak,
                sakType = sak.sakType,
            )

        return brevbakerService.genererPdf(brev.id, brevRequest)
            .also { lagrePdfHvisVedtakFattet(generellBrevData, brev, it) }
    }

    private fun hentLagretInnhold(brev: Brev) =
        requireNotNull(
            db.hentBrevPayload(brev.id),
        ) { "Fant ikke payload for brev ${brev.id}" }.elements

    private fun hentLagretInnholdVedlegg(brev: Brev) =
        requireNotNull(db.hentBrevPayloadVedlegg(brev.id)) {
            "Fant ikke payloadvedlegg for brev ${brev.id}"
        }

    private fun sjekkOmBrevKanEndres(brevID: BrevID): Brev {
        val brev = db.hentBrev(brevID)
        if (!brev.kanEndres()) {
            throw IllegalStateException("Brev med id=$brevID kan ikke endres, siden det har status ${brev.status}")
        }
        return brev
    }
}
