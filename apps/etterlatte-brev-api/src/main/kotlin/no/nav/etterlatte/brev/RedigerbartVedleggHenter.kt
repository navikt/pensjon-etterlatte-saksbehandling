package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class RedigerbartVedleggHenter(
    private val brevbakerService: BrevbakerService,
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
                    VedtakType.INNVILGELSE ->
                        vedleggInnvilgelseOmstillingsstoenad(
                            bruker,
                            generellBrevData,
                            generellBrevData.personerISak.soekerOgEventuellVerge(),
                            generellBrevData.sak.id,
                            generellBrevData.spraak,
                            generellBrevData.sak.sakType,
                            generellBrevData.forenkletVedtak,
                            generellBrevData.sak.enhet,
                        )
                    VedtakType.OPPHOER ->
                        vedleggOpphoerOmstillingsstoenad(
                            bruker,
                            generellBrevData,
                            generellBrevData.personerISak.soekerOgEventuellVerge(),
                            generellBrevData.sak.id,
                            generellBrevData.spraak,
                            generellBrevData.sak.sakType,
                            generellBrevData.forenkletVedtak,
                            generellBrevData.sak.enhet,
                            generellBrevData.behandlingId,
                        )
                    VedtakType.ENDRING -> {
                        if (brevtype == Brevtype.VARSEL && generellBrevData.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                            emptyList()
                        } else {
                            vedleggEndringOmstillingsstoenad(
                                bruker,
                                generellBrevData,
                                generellBrevData.personerISak.soekerOgEventuellVerge(),
                                generellBrevData.sak.id,
                                generellBrevData.spraak,
                                generellBrevData.sak.sakType,
                                generellBrevData.forenkletVedtak,
                                generellBrevData.sak.enhet,
                                generellBrevData.behandlingId,
                            )
                        }
                    }
                    else -> {
                        if (brevtype == Brevtype.VARSEL) {
                            listOf(
                                hentInnholdBeregningVedleggOms(
                                    bruker,
                                    generellBrevData,
                                    generellBrevData.personerISak.soekerOgEventuellVerge(),
                                    generellBrevData.sak.id,
                                    generellBrevData.spraak,
                                    generellBrevData.sak.sakType,
                                    generellBrevData.forenkletVedtak,
                                    generellBrevData.sak.enhet,
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
                        vedleggInnvilgelseBarnepensjon(
                            bruker,
                            generellBrevData,
                            generellBrevData.personerISak.soekerOgEventuellVerge(),
                            generellBrevData.sak.id,
                            generellBrevData.spraak,
                            generellBrevData.sak.sakType,
                            generellBrevData.forenkletVedtak,
                            generellBrevData.sak.enhet,
                        )
                    VedtakType.OPPHOER ->
                        vedleggOpphoerBarnepensjon(
                            bruker,
                            generellBrevData,
                            generellBrevData.personerISak.soekerOgEventuellVerge(),
                            generellBrevData.sak.id,
                            generellBrevData.spraak,
                            generellBrevData.sak.sakType,
                            generellBrevData.forenkletVedtak,
                            generellBrevData.sak.enhet,
                            generellBrevData.behandlingId,
                        )
                    VedtakType.ENDRING ->
                        vedleggEndringBarnepensjon(
                            bruker,
                            generellBrevData,
                            generellBrevData.personerISak.soekerOgEventuellVerge(),
                            generellBrevData.sak.id,
                            generellBrevData.spraak,
                            generellBrevData.sak.sakType,
                            generellBrevData.forenkletVedtak,
                            generellBrevData.sak.enhet,
                            generellBrevData.behandlingId,
                        )
                    else -> {
                        if (brevtype == Brevtype.VARSEL) {
                            listOf(
                                hentInnholdBeregningAvTrygdetidBp(
                                    bruker,
                                    generellBrevData,
                                    generellBrevData.personerISak.soekerOgEventuellVerge(),
                                    generellBrevData.sak.id,
                                    generellBrevData.spraak,
                                    generellBrevData.sak.sakType,
                                    generellBrevData.forenkletVedtak,
                                    generellBrevData.sak.enhet,
                                ),
                            )
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
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
    ) = listOf(
        hentInnholdBeregningVedleggOms(
            bruker,
            generellBrevData,
            soekerOgEventuellVerge,
            sakId,
            spraak,
            sakType,
            forenkletVedtak,
            enhet,
        ),
    )

    private suspend fun vedleggOpphoerOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
        behandlingId: UUID?,
    ) = if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
        listOf(
            hentInnholdForhaandsvarselFeilutbetalingVedleggOms(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
        )
    } else {
        emptyList()
    }

    private suspend fun vedleggEndringOmstillingsstoenad(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
        behandlingId: UUID?,
    ) = if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
        listOf(
            hentInnholdBeregningVedleggOms(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
            hentInnholdForhaandsvarselFeilutbetalingVedleggOms(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
        )
    } else {
        listOf(
            hentInnholdBeregningVedleggOms(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
        )
    }

    private suspend fun vedleggInnvilgelseBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
    ) = listOf(
        hentInnholdBeregningAvTrygdetidBp(
            bruker,
            generellBrevData,
            soekerOgEventuellVerge,
            sakId,
            spraak,
            sakType,
            forenkletVedtak,
            enhet,
        ),
    )

    private suspend fun vedleggEndringBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
        behandlingId: UUID?,
    ) = if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
        listOf(
            hentInnholdBeregningAvTrygdetidBp(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
            hentInnholdForhaandsvarselFeilutbetalingVedleggBp(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
        )
    } else {
        listOf(
            hentInnholdBeregningAvTrygdetidBp(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
        )
    }

    private suspend fun vedleggOpphoerBarnepensjon(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
        behandlingId: UUID?,
    ) = if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
        listOf(
            hentInnholdForhaandsvarselFeilutbetalingVedleggBp(
                bruker,
                generellBrevData,
                soekerOgEventuellVerge,
                sakId,
                spraak,
                sakType,
                forenkletVedtak,
                enhet,
            ),
        )
    } else {
        emptyList()
    }

    private suspend fun hentInnholdBeregningVedleggOms(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        s: String,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
        BrevVedleggKey.OMS_BEREGNING,
        generellBrevData,
        bruker,
        soekerOgEventuellVerge,
        sakId,
        spraak,
        sakType,
        forenkletVedtak,
        s,
    )

    private suspend fun hentInnholdForhaandsvarselFeilutbetalingVedleggOms(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL,
        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
        generellBrevData,
        bruker,
        soekerOgEventuellVerge,
        sakId,
        spraak,
        sakType,
        forenkletVedtak,
        enhet,
    )

    private suspend fun hentInnholdForhaandsvarselFeilutbetalingVedleggBp(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL,
        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
        generellBrevData,
        bruker,
        soekerOgEventuellVerge,
        sakId,
        spraak,
        sakType,
        forenkletVedtak,
        enhet,
    )

    private suspend fun hentInnholdBeregningAvTrygdetidBp(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
    ) = hentInnholdFraBrevbakeren(
        EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
        generellBrevData,
        bruker,
        soekerOgEventuellVerge,
        sakId,
        spraak,
        sakType,
        forenkletVedtak,
        enhet,
    )

    private suspend fun hentInnholdFraBrevbakeren(
        kode: EtterlatteBrevKode,
        key: BrevVedleggKey,
        generellBrevData: GenerellBrevData,
        bruker: BrukerTokenInfo,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        spraak: Spraak,
        sakType: SakType,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
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
                        soekerOgEventuellVerge = soekerOgEventuellVerge,
                        sakId = sakId,
                        spraak = spraak,
                        sakType = sakType,
                        forenkletVedtak = forenkletVedtak,
                        enhet = enhet,
                    ),
                ),
        )

    private suspend fun harFeilutbetalingMedVarsel(
        bruker: BrukerTokenInfo,
        behandlingId: UUID?,
    ): Boolean =
        coroutineScope {
            val brevutfall = async { behandlingId?.let { behandlingService.hentBrevutfall(it, bruker) } }
            val brevutfallHentet = requireNotNull(brevutfall.await())
            brevutfallHentet.feilutbetaling?.valg == FeilutbetalingValg.JA_VARSEL
        }
}
