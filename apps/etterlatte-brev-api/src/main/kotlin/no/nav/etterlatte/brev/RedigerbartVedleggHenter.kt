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
    ): List<BrevInnholdVedlegg> {
        val brevkoder: List<Pair<EtterlatteBrevKode, BrevVedleggKey>> =
            when (generellBrevData.sak.sakType) {
                SakType.OMSTILLINGSSTOENAD -> {
                    when (generellBrevData.forenkletVedtak?.type) {
                        VedtakType.INNVILGELSE ->
                            listOf(EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL to BrevVedleggKey.OMS_BEREGNING)

                        VedtakType.OPPHOER -> {
                            if (harFeilutbetalingMedVarsel(bruker, generellBrevData.behandlingId)) {
                                listOf(
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL to
                                        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                                )
                            } else {
                                emptyList()
                            }
                        }

                        VedtakType.ENDRING -> {
                            if (brevtype == Brevtype.VARSEL && generellBrevData.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                                emptyList()
                            } else if (harFeilutbetalingMedVarsel(bruker, generellBrevData.behandlingId)) {
                                listOf(
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL to BrevVedleggKey.OMS_BEREGNING,
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL to
                                        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                                )
                            } else {
                                listOf(
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL to BrevVedleggKey.OMS_BEREGNING,
                                )
                            }
                        }

                        else -> {
                            if (brevtype == Brevtype.VARSEL) {
                                listOf(EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL to BrevVedleggKey.OMS_BEREGNING)
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
                                EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL to BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                            )
                        VedtakType.OPPHOER -> {
                            if (harFeilutbetalingMedVarsel(bruker, generellBrevData.behandlingId)) {
                                listOf(
                                    EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL to
                                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                                )
                            } else {
                                listOf()
                            }
                        }
                        VedtakType.ENDRING -> {
                            if (harFeilutbetalingMedVarsel(bruker, generellBrevData.behandlingId)) {
                                listOf(
                                    EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL to
                                        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                    EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL to
                                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                                )
                            } else {
                                listOf(
                                    EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL to
                                        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                )
                            }
                        }
                        else -> {
                            if (brevtype == Brevtype.VARSEL) {
                                listOf(
                                    EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL to
                                        BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                                )
                            }
                            emptyList()
                        }
                    }
                }
            }

        return brevkoder
            .map {
                hentInnholdFraBrevbakeren(
                    kode = it.first,
                    key = it.second,
                    generellBrevData = generellBrevData,
                    bruker = bruker,
                    soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                    sakId = generellBrevData.sak.id,
                    spraak = generellBrevData.spraak,
                    sakType = generellBrevData.sak.sakType,
                    forenkletVedtak = generellBrevData.forenkletVedtak,
                    enhet = generellBrevData.sak.enhet,
                )
            }.toList()
    }

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
                        brukerTokenInfo = bruker,
                        brevkode = kode,
                        brevdata = { ManueltBrevData() },
                        soekerOgEventuellVerge = soekerOgEventuellVerge,
                        sakId = sakId,
                        spraak = spraak,
                        sakType = sakType,
                        forenkletVedtak = forenkletVedtak,
                        enhet = enhet,
                        utlandstilknytningType = generellBrevData.utlandstilknytning?.type,
                        revurderingaarsak = generellBrevData.revurderingsaarsak,
                        behandlingId = generellBrevData.behandlingId,
                        erForeldreloes = generellBrevData.erForeldreloes(),
                        loependeIPesys = generellBrevData.loependeIPesys(),
                        systemkilde = generellBrevData.systemkilde,
                        avdoede = generellBrevData.personerISak.avdoede,
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
