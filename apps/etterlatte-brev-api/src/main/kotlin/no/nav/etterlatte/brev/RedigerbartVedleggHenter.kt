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
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
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
    private val adresseService: AdresseService,
    private val behandlingService: BehandlingService,
) {
    suspend fun hentInitiellPayloadVedlegg(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        brevtype: Brevtype,
    ): List<BrevInnholdVedlegg> =
        when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> vedleggInnvilgelseOmstillingsstoenad(bruker, generellBrevData)
                    VedtakType.OPPHOER -> vedleggOpphoerOmstillingsstoenad(bruker, generellBrevData)
                    VedtakType.ENDRING -> {
                        if (brevtype == Brevtype.VARSEL && generellBrevData.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                            emptyList()
                        } else {
                            vedleggEndringOmstillingsstoenad(bruker, generellBrevData)
                        }
                    }
                    else -> {
                        if (brevtype == Brevtype.VARSEL) {
                            listOf(hentInnholdBeregningVedleggOms(bruker, generellBrevData))
                        } else {
                            emptyList()
                        }
                    }
                }
            }

            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> vedleggInnvilgelseBarnepensjon(bruker, generellBrevData)
                    VedtakType.OPPHOER -> vedleggOpphoerBarnepensjon(bruker, generellBrevData)
                    VedtakType.ENDRING -> vedleggEndringBarnepensjon(bruker, generellBrevData)
                    else -> {
                        if (brevtype == Brevtype.VARSEL) {
                            listOf(hentInnholdBeregningAvTrygdetidBp(bruker, generellBrevData))
                        } else {
                            emptyList()
                        }
                    }
                }
            }
        }

    private suspend fun vedleggInnvilgelseOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(hentInnholdBeregningVedleggOms(bruker, generellBrevData))

    private suspend fun vedleggOpphoerOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
        listOf(hentInnholdForhaandsvarselFeilutbetalingVedleggOms(bruker, generellBrevData))
    } else {
        emptyList()
    }

    private suspend fun vedleggEndringOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
        listOf(
            hentInnholdBeregningVedleggOms(bruker, generellBrevData),
            hentInnholdForhaandsvarselFeilutbetalingVedleggOms(bruker, generellBrevData),
        )
    } else {
        listOf(hentInnholdBeregningVedleggOms(bruker, generellBrevData))
    }

    private suspend fun vedleggInnvilgelseBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(hentInnholdBeregningAvTrygdetidBp(bruker, generellBrevData))

    private suspend fun vedleggEndringBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
        listOf(
            hentInnholdBeregningAvTrygdetidBp(bruker, generellBrevData),
            hentInnholdForhaandsvarselFeilutbetalingVedleggBp(bruker, generellBrevData),
        )
    } else {
        listOf(
            hentInnholdBeregningAvTrygdetidBp(bruker, generellBrevData),
        )
    }

    private suspend fun vedleggOpphoerBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = if (harFeilutbetalingMedVarsel(bruker, generellBrevData)) {
        listOf(hentInnholdForhaandsvarselFeilutbetalingVedleggBp(bruker, generellBrevData))
    } else {
        emptyList()
    }

    private suspend fun hentInnholdBeregningVedleggOms(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
        BrevVedleggKey.OMS_BEREGNING,
        generellBrevData,
        bruker,
    )

    private suspend fun hentInnholdForhaandsvarselFeilutbetalingVedleggOms(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL,
        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
        generellBrevData,
        bruker,
    )

    private suspend fun hentInnholdForhaandsvarselFeilutbetalingVedleggBp(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL,
        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
        generellBrevData,
        bruker,
    )

    private suspend fun hentInnholdBeregningAvTrygdetidBp(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
        generellBrevData,
        bruker,
    )

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
            val brevutfall = async { generellBrevData.behandlingId?.let { behandlingService.hentBrevutfall(it, bruker) } }
            val brevutfallHentet = requireNotNull(brevutfall.await())
            brevutfallHentet.feilutbetaling?.valg == FeilutbetalingValg.JA_VARSEL
        }
}
