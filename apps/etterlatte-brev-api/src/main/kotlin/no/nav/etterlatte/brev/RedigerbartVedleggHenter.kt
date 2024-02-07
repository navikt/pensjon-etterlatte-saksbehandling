package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.ManueltBrevData
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
                    VedtakType.INNVILGELSE -> innvilgelseOMS(bruker, generellBrevData)
                    VedtakType.ENDRING -> endringOMS(bruker, generellBrevData)
                    else -> null
                }
            }

            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> innvilgelseBP(bruker, generellBrevData)
                    VedtakType.ENDRING -> endringBP(bruker, generellBrevData)
                    else -> null
                }
            }
        }

    private suspend fun innvilgelseOMS(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(utfallBeregningOMS(bruker, generellBrevData))

    private suspend fun endringOMS(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(utfallBeregningOMS(bruker, generellBrevData))

    private suspend fun innvilgelseBP(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(beregningAvBarnepensjonTrygdetid(bruker, generellBrevData))

    private suspend fun endringBP(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = listOf(beregningAvBarnepensjonTrygdetid(bruker, generellBrevData))

    private suspend fun utfallBeregningOMS(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
        BrevVedleggKey.BEREGNING_INNHOLD,
        generellBrevData,
        bruker,
    )

    private suspend fun beregningAvBarnepensjonTrygdetid(
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
                        brevkode = kode,
                        brevdata = { ManueltBrevData() },
                    ),
                ),
        )
}
