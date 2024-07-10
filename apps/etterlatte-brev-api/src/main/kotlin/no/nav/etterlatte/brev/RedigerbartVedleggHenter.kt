package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest.Companion.finnVergesNavn
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
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
        sakId: Long,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
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
                hentInnholdFraBrevbakeren(
                    kode = it.first,
                    key = it.second,
                    bruker = bruker,
                    soekerOgEventuellVerge = soekerOgEventuellVerge,
                    sakId = sakId,
                    forenkletVedtak = forenkletVedtak,
                    enhet = enhet,
                    sakType = sakType,
                    spraak = spraak,
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
                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                            BrevVedleggKey.OMS_BEREGNING,
                        ),
                    )

                VedtakType.OPPHOER ->
                    if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
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
                    if (brevtype == Brevtype.VARSEL && revurderingaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                        emptyList()
                    } else {
                        if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
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
            when (vedtakType) {
                VedtakType.INNVILGELSE ->
                    listOf(
                        Pair(
                            EtterlatteBrevKode.BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL,
                            BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                        ),
                    )

                VedtakType.OPPHOER ->
                    if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
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
                    if (harFeilutbetalingMedVarsel(bruker, behandlingId)) {
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

    private suspend fun hentInnholdFraBrevbakeren(
        kode: EtterlatteBrevKode,
        key: BrevVedleggKey,
        bruker: BrukerTokenInfo,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        sakId: Long,
        forenkletVedtak: ForenkletVedtak?,
        enhet: String,
        sakType: SakType,
        spraak: Spraak,
    ): BrevInnholdVedlegg =
        BrevInnholdVedlegg(
            tittel = kode.tittel!!,
            key = key,
            payload =
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(
                    BrevbakerRequest.fra(
                        kode = kode,
                        letterData = ManueltBrevData(),
                        felles =
                            BrevbakerHelpers.mapFelles(
                                sakId = sakId,
                                soeker = soekerOgEventuellVerge.soeker,
                                avsender =
                                    adresseService.hentAvsender(
                                        opprettAvsenderRequest(bruker, forenkletVedtak, enhet),
                                    ),
                                vergeNavn =
                                    finnVergesNavn(
                                        kode,
                                        soekerOgEventuellVerge,
                                        sakType,
                                    ),
                            ),
                        spraak = spraak,
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
