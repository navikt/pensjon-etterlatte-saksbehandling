package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo

class RedigerbartVedleggHenter(private val brevbakerService: BrevbakerService) {
    suspend fun hentInitiellPayloadVedlegg(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ): List<BrevInnholdVedlegg>? =
        when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> vedleggInnvilgelseOmstillingsstoenad(bruker, generellBrevData)
                    VedtakType.ENDRING -> vedleggEndringOmstillingsstoenad(bruker, generellBrevData)
                    else -> null
                }
            }

            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> vedleggInnvilgelseBarnepensjon(bruker, generellBrevData)
                    VedtakType.ENDRING -> vedleggEndringBarnepensjon(bruker, generellBrevData)
                    else -> null
                }
            }
        }

    private suspend fun vedleggInnvilgelseOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(hentInnholdBeregningVedleggOms(bruker, generellBrevData))

    private suspend fun vedleggEndringOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(
        hentInnholdBeregningVedleggOms(bruker, generellBrevData),
        hentInnholdForhaandsvarselFeilutbetalingVedleggOms(bruker, generellBrevData),
    )

    private suspend fun vedleggInnvilgelseBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(hentInnholdBeregningAvTrygdetidBp(bruker, generellBrevData))

    private suspend fun vedleggEndringBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(hentInnholdBeregningAvTrygdetidBp(bruker, generellBrevData))

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
    ): BrevInnholdVedlegg =
        BrevInnholdVedlegg(
            tittel = kode.tittel!!,
            key = key,
            payload =
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(
                    RedigerbarTekstRequest(
                        generellBrevData = generellBrevData,
                        brukerTokenInfo = bruker,
                        brevkode = { _, _ -> kode },
                    ),
                ),
        )
}
