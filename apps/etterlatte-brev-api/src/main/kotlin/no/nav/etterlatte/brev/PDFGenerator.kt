package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevDataMapper.BrevkodePar
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverk
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverkFerdig
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PDFGenerator(
    private val db: BrevRepository,
    private val brevDataFacade: BrevdataFacade,
    private val brevDataMapper: BrevDataMapper,
    private val adresseService: AdresseService,
    private val brevbakerService: BrevbakerService,
    private val migreringBrevDataService: MigreringBrevDataService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest?,
        avsenderRequest: (GenerellBrevData) -> AvsenderRequest,
        brevKode: (GenerellBrevData, Brev, MigreringBrevRequest?) -> BrevkodePar,
        lagrePdfHvisVedtakFattet: (GenerellBrevData, Brev, Pdf) -> Unit = { _, _, _ -> run {} },
    ): Pdf {
        val brev = db.hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id)) { "Fant ikke brev med id ${brev.id}" }
        }

        val generellBrevData =
            retryOgPakkUt { brevDataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId, bruker) }
        val avsender = adresseService.hentAvsender(avsenderRequest(generellBrevData))

        val brevkodePar = brevKode(generellBrevData, brev, automatiskMigreringRequest)

        val sak = generellBrevData.sak
        val letterData =
            brevData(
                generellBrevData,
                automatiskMigreringRequest,
                brev,
                bruker,
                brevkodePar,
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

        return brevbakerService.genererPdf(brev.id, brevRequest).let {
            when (letterData) {
                is OmregnetBPNyttRegelverkFerdig -> {
                    val forhaandsvarsel =
                        brevbakerService.genererPdf(
                            brev.id,
                            BrevbakerRequest.fra(
                                EtterlatteBrevKode.BARNEPENSJON_FORHAANDSVARSEL_OMREGNING,
                                letterData.data,
                                avsender,
                                generellBrevData.personerISak.soekerOgEventuellVerge(),
                                sak.id,
                                generellBrevData.spraak,
                                sak.sakType,
                            ),
                        )
                    forhaandsvarsel.medPdfAppended(it)
                }

                else -> it
            }
        }.also { lagrePdfHvisVedtakFattet(generellBrevData, brev, it) }
    }

    private suspend fun brevData(
        generellBrevData: GenerellBrevData,
        automatiskMigreringRequest: MigreringBrevRequest?,
        brev: Brev,
        brukerTokenInfo: BrukerTokenInfo,
        brevkodePar: BrevkodePar,
    ): BrevData {
        if (brevkodePar.erInformasjonsbrev()) {
            return ManueltBrevMedTittelData(
                requireNotNull(db.hentBrevPayload(brev.id)).elements,
                brev.tittel,
            )
        }
        return when (
            generellBrevData.systemkilde == Vedtaksloesning.PESYS ||
                automatiskMigreringRequest?.erOmregningGjenny ?: false
        ) {
            false -> opprettBrevData(brev, generellBrevData, brukerTokenInfo, brevkodePar)
            true ->
                OmregnetBPNyttRegelverkFerdig(
                    innhold =
                        InnholdMedVedlegg(
                            { hentLagretInnhold(brev) },
                            { hentLagretInnholdVedlegg(brev) },
                        ).innhold(),
                    data = (
                        migreringBrevDataService.opprettMigreringBrevdata(
                            generellBrevData,
                            automatiskMigreringRequest,
                            brukerTokenInfo,
                        ) as OmregnetBPNyttRegelverk
                    ),
                )
        }
    }

    private suspend fun opprettBrevData(
        brev: Brev,
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        brevkode: BrevkodePar,
    ): BrevData =
        when (brev.prosessType) {
            BrevProsessType.REDIGERBAR ->
                brevDataMapper.brevDataFerdigstilling(
                    generellBrevData,
                    brukerTokenInfo,
                    InnholdMedVedlegg({ hentLagretInnhold(brev) }, { hentLagretInnholdVedlegg(brev) }),
                    brevkode,
                )

            BrevProsessType.AUTOMATISK -> brevDataMapper.brevData(generellBrevData, brukerTokenInfo)
            BrevProsessType.MANUELL -> ManueltBrevData(hentLagretInnhold(brev))
        }

    private fun hentLagretInnhold(brev: Brev) =
        requireNotNull(
            db.hentBrevPayload(brev.id),
        ) { "Fant ikke payload for brev ${brev.id}" }.elements

    private fun hentLagretInnholdVedlegg(brev: Brev) =
        requireNotNull(db.hentBrevPayloadVedlegg(brev.id)) {
            "Fant ikke payloadvedlegg for brev ${brev.id}"
        }
}
