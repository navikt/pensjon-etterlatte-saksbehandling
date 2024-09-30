package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class RedigerbartVedleggHenter(
    private val brevbakerService: BrevbakerService,
    private val adresseService: AdresseService,
    private val behandlingService: BehandlingService,
) {
    suspend fun hentInitiellPayloadVedlegg(
        bruker: BrukerTokenInfo,
        brevtype: Brevtype,
        sakType: SakType,
        vedtakType: VedtakType?,
        behandlingId: UUID?,
        revurderingaarsak: Revurderingaarsak?,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: SakId,
        forenkletVedtak: ForenkletVedtak?,
        enhet: Enhetsnummer,
        spraak: Spraak,
    ): List<BrevInnholdVedlegg> {
        val vedlegg =
            finnVedlegg(
                bruker,
                brevtype,
                sakType,
                vedtakType,
                behandlingId,
                revurderingaarsak,
            )
        return vedlegg
            .map {
                BrevInnholdVedlegg(
                    tittel = it.first.tittel,
                    key = it.second,
                    payload =
                        brevbakerService.hentRedigerbarTekstFraBrevbakeren(
                            BrevbakerRequest.fra(
                                brevKode = it.first,
                                brevData = ManueltBrevData(),
                                avsender =
                                    adresseService.hentAvsender(
                                        opprettAvsenderRequest(bruker, forenkletVedtak, enhet),
                                    ),
                                soekerOgEventuellVerge = soekerOgEventuellVerge,
                                sakId = sakId,
                                spraak = spraak,
                                sakType = sakType,
                            ),
                        ),
                )
            }.toList()
    }

    private suspend fun RedigerbartVedleggHenter.finnVedlegg(
        bruker: BrukerTokenInfo,
        brevtype: Brevtype,
        sakType: SakType,
        vedtakType: VedtakType?,
        behandlingId: UUID?,
        revurderingaarsak: Revurderingaarsak?,
    ) = when (sakType) {
        SakType.OMSTILLINGSSTOENAD -> {
            when (vedtakType) {
                VedtakType.INNVILGELSE ->
                    listOf(
                        Pair(
                            Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                            BrevVedleggKey.OMS_BEREGNING,
                        ),
                    )

                VedtakType.OPPHOER ->
                    if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
                        listOf(
                            Pair(
                                Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                            ),
                        )
                    } else {
                        emptyList()
                    }

                VedtakType.ENDRING -> {
                    if (brevtype == Brevtype.VARSEL && revurderingaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                        emptyList()
                    } else {
                        if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
                            listOf(
                                Pair(
                                    Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                                    BrevVedleggKey.OMS_BEREGNING,
                                ),
                                Pair(
                                    Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                    BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                                ),
                            )
                        } else {
                            listOf(
                                Pair(
                                    Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
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
                                Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
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
            when (vedtakType) {
                VedtakType.INNVILGELSE ->
                    listOf(
                        Pair(
                            Vedlegg.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                            BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                        ),
                    )

                VedtakType.OPPHOER ->
                    if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
                        listOf(
                            Pair(
                                Vedlegg.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                            ),
                        )
                    } else {
                        emptyList()
                    }

                VedtakType.ENDRING ->
                    if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
                        listOf(
                            Pair(
                                Vedlegg.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                                BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                            ),
                            Pair(
                                Vedlegg.BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL,
                                BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                            ),
                        )
                    } else {
                        listOf(
                            Pair(
                                Vedlegg.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                                BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                            ),
                        )
                    }

                else -> {
                    if (brevtype == Brevtype.VARSEL) {
                        listOf(
                            Pair(
                                Vedlegg.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
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
