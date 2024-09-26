package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.erForeldreloes
import no.nav.etterlatte.brev.behandling.loependeIPesys
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.formaterNavn
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
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInntektsjustering
import no.nav.etterlatte.brev.vedtaksbrev.UgyldigMottakerKanIkkeFerdigstilles
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PDFGenerator(
    private val db: BrevRepository,
    private val brevDataFacade: BrevdataFacade,
    private val adresseService: AdresseService,
    private val brevbakerService: BrevbakerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogger = sikkerlogger()

    suspend fun ferdigstillOgGenererPDF(
        id: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, Enhetsnummer) -> AvsenderRequest,
        brevKodeMapping: (BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataFerdigstillingRequest) -> BrevDataFerdigstilling,
        lagrePdfHvisVedtakFattet: (VedtakStatus?, String?, Brev, Pdf) -> Unit = { _, _, _, _ -> run {} },
    ): Pdf {
        val brev = sjekkOmBrevKanEndres(id)
        if (!brev.mottaker.erGyldig()) {
            sikkerlogger.error("Ugyldig mottaker: ${brev.mottaker.toJson()}")
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id)
        }

        val pdf =
            genererPdf(
                id,
                bruker,
                avsenderRequest,
                brevKodeMapping,
                brevDataMapping,
                lagrePdfHvisVedtakFattet,
            )
        db.lagrePdfOgFerdigstillBrev(id, pdf)
        return pdf
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, Enhetsnummer) -> AvsenderRequest,
        brevKodeMapping: (BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataFerdigstillingRequest) -> BrevDataFerdigstilling,
        lagrePdfHvisVedtakFattet: (VedtakStatus?, String?, Brev, Pdf) -> Unit = { _, _, _, _ -> run {} },
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
        val avsender = adresseService.hentAvsender(avsenderRequest(bruker, generellBrevData.forenkletVedtak, generellBrevData.sak.enhet))

        val brevkodePar =
            brevKodeMapping(
                BrevkodeRequest(
                    loependeIPesys(generellBrevData.systemkilde, generellBrevData.behandlingId, generellBrevData.revurderingsaarsak),
                    erForeldreloes(generellBrevData.personerISak.soeker, generellBrevData.personerISak.avdoede),
                    generellBrevData.sak.sakType,
                    generellBrevData.forenkletVedtak?.type,
                ),
            )

        val sak = generellBrevData.sak
        val brevRequest =
            BrevbakerRequest.fra(
                brevKode = brevkodePar.ferdigstilling,
                brevData =
                    brevDataMapping(
                        BrevDataFerdigstillingRequest(
                            loependeIPesys =
                                loependeIPesys(
                                    generellBrevData.systemkilde,
                                    generellBrevData.behandlingId,
                                    generellBrevData.revurderingsaarsak,
                                ),
                            behandlingId = generellBrevData.behandlingId,
                            sakType = generellBrevData.sak.sakType,
                            erForeldreloes = erForeldreloes(generellBrevData.personerISak.soeker, generellBrevData.personerISak.avdoede),
                            utlandstilknytningType = generellBrevData.utlandstilknytning?.type,
                            avdoede = generellBrevData.personerISak.avdoede,
                            systemkilde = generellBrevData.systemkilde,
                            soekerUnder18 = generellBrevData.personerISak.soeker.under18,
                            soekerNavn = generellBrevData.personerISak.soeker.formaterNavn(),
                            sakId = generellBrevData.sak.id,
                            virkningstidspunkt = generellBrevData.forenkletVedtak?.virkningstidspunkt,
                            vedtakType = generellBrevData.forenkletVedtak?.type,
                            revurderingsaarsak = generellBrevData.revurderingsaarsak,
                            tilbakekreving = generellBrevData.forenkletVedtak?.tilbakekreving,
                            klage = generellBrevData.forenkletVedtak?.klage,
                            harVerge = generellBrevData.personerISak.verge != null,
                            bruker = bruker,
                            innholdMedVedlegg = InnholdMedVedlegg({ hentLagretInnhold(brev) }, { hentLagretInnholdVedlegg(brev) }),
                            kode = brevkodePar,
                            tittel = brev.tittel,
                        ),
                    ),
                avsender = avsender,
                soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                sakId = sak.id,
                spraak = generellBrevData.spraak,
                sakType = sak.sakType,
            )

        return brevbakerService
            .genererPdf(brev.id, brevRequest)
            .let {
                // TODO: finne et bedre sted for dette?
                when (brev.brevkoder) {
                    Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL -> {
                        val vedtaksbrev =
                            brevbakerService.genererPdf(
                                brev.id,
                                BrevbakerRequest.fra(
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING, // TODO: gjennbrue eller egen mal for vedtaksbrev
                                    OmstillingsstoenadInntektsjustering(), // TODO: må ha riktig data iht brevkode
                                    avsender,
                                    generellBrevData.personerISak.soekerOgEventuellVerge(),
                                    sak.id,
                                    generellBrevData.spraak,
                                    sak.sakType,
                                ),
                            )
                        PDFService.kombinerPdfListeTilEnPdf(listOf(vedtaksbrev, it))
                    }
                    else -> it
                }
            }.also {
                lagrePdfHvisVedtakFattet(
                    generellBrevData.forenkletVedtak?.status,
                    generellBrevData.forenkletVedtak?.saksbehandlerIdent,
                    brev,
                    it,
                )
            }
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
