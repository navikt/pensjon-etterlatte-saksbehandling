package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest.Companion.finnVergesNavn
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class RedigerbartVedleggHenter(
    private val brevbakerService: BrevbakerService,
    private val brevdataFacade: BrevdataFacade,
    private val adresseService: AdresseService,
) {
    suspend fun hentInitiellPayloadVedlegg(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        brevtype: Brevtype,
    ): List<BrevInnholdVedlegg> {
        val vedlegg: List<Pair<EtterlatteBrevKode, BrevVedleggKey>> =
            when (generellBrevData.sak.sakType) {
                SakType.OMSTILLINGSSTOENAD -> {
                    when (generellBrevData.forenkletVedtak?.type) {
                        VedtakType.INNVILGELSE ->
                            listOf(
                                Pair(
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                                    BrevVedleggKey.OMS_BEREGNING,
                                ),
                            )

                        VedtakType.OPPHOER ->
                            if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
                                listOf(
                                    Pair(
                                        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                                    ),
                                )
                            } else {
                                emptyList()
                            }

                        VedtakType.ENDRING -> {
                            if (brevtype == Brevtype.VARSEL && generellBrevData.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                                emptyList()
                            } else {
                                if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
                                    listOf(
                                        Pair(
                                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                                            BrevVedleggKey.OMS_BEREGNING,
                                        ),
                                        Pair(
                                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                            BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                                        ),
                                    )
                                } else {
                                    listOf(
                                        Pair(
                                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                                            BrevVedleggKey.OMS_BEREGNING,
                                        ),
                                    )
                                }
                            }
                        }

                        else -> {
                            if (brevtype == Brevtype.VARSEL) {
                                listOf(
                                    Pair(
                                        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                                        BrevVedleggKey.OMS_BEREGNING,
                                    ),
                                )
                            } else {
                                emptyList()
                            }
                        }
                    }
                }

                SakType.BARNEPENSJON -> {
                    when (generellBrevData.forenkletVedtak?.type) {
                        VedtakType.INNVILGELSE ->
                            listOf(
                                Pair(
                                    EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                                    BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                ),
                            )

                        VedtakType.OPPHOER ->
                            if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
                                listOf(
                                    Pair(
                                        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                                    ),
                                )
                            } else {
                                emptyList()
                            }

                        VedtakType.ENDRING ->
                            if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
                                listOf(
                                    Pair(
                                        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                                        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                    ),
                                    Pair(
                                        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                                    ),
                                )
                            } else {
                                listOf(
                                    Pair(
                                        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                                        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                    ),
                                )
                            }

                        else -> {
                            if (brevtype == Brevtype.VARSEL) {
                                listOf(
                                    Pair(
                                        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                                        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                    ),
                                )
                            } else {
                                emptyList()
                            }
                        }
                    }
                }
            }
        return vedlegg
            .map {
                hentInnholdFraBrevbakeren(
                    kode = it.first,
                    key = it.second,
                    generellBrevData = generellBrevData,
                    bruker = bruker,
                )
            }.toList()
    }

    private suspend fun hentInnholdFraBrevbakeren(
        kode: EtterlatteBrevKode,
        key: BrevVedleggKey,
        generellBrevData: GenerellBrevData,
        bruker: BrukerTokenInfo,
    ): BrevInnholdVedlegg {
        val soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge()
        return BrevInnholdVedlegg(
            tittel = kode.tittel!!,
            key = key,
            payload =
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(
                    BrevbakerRequest.fra(
                        kode = kode,
                        letterData = ManueltBrevData(),
                        felles =
                            BrevbakerHelpers.mapFelles(
                                sakId = generellBrevData.sak.id,
                                soeker = generellBrevData.personerISak.soeker,
                                avsender =
                                    adresseService.hentAvsender(
                                        opprettAvsenderRequest(bruker, generellBrevData.forenkletVedtak, generellBrevData.sak.enhet),
                                    ),
                                vergeNavn =
                                    finnVergesNavn(
                                        kode,
                                        soekerOgEventuellVerge,
                                        generellBrevData.sak.sakType,
                                    ),
                            ),
                        spraak = generellBrevData.spraak,
                    ),
                ),
        )
    }

    private suspend fun harFeilutbetalingMedVarsel(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ): Boolean =
        coroutineScope {
            val brevutfall = async { generellBrevData.behandlingId?.let { brevdataFacade.hentBrevutfall(it, bruker) } }
            val brevutfallHentet = requireNotNull(brevutfall.await())
            brevutfallHentet.feilutbetaling?.valg == FeilutbetalingValg.JA_VARSEL
        }
}
