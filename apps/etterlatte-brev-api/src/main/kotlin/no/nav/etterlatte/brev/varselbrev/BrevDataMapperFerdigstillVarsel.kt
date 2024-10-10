package no.nav.etterlatte.brev.varselbrev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.ManueltBrevMedTittelData
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarsel
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregning
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregningsperioder
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAktivitetspliktVarsel
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.YearMonth
import java.util.UUID

class BrevDataMapperFerdigstillVarsel(
    private val beregningService: BeregningService,
    private val trygdetidService: TrygdetidService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) {
    suspend fun hentBrevDataFerdigstilling(request: BrevDataFerdigstillingRequest) =
        coroutineScope {
            when (request.sakType) {
                SakType.BARNEPENSJON -> hentBrevDataFerdigstillingBarnepensjon(request)
                SakType.OMSTILLINGSSTOENAD -> hentBrevdataFerdigstillingOmstillingsstoenad(request)
            }
        }

    private suspend fun hentBrevdataFerdigstillingOmstillingsstoenad(request: BrevDataFerdigstillingRequest) =
        coroutineScope {
            if (request.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                OmstillingsstoenadAktivitetspliktVarsel(
                    request.innholdMedVedlegg.innhold(),
                    request.utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                )
            } else {
                ManueltBrevMedTittelData(
                    request.innholdMedVedlegg.innhold(),
                    Brevkoder.OMS_VARSEL.tittel,
                )
            }
        }

    private suspend fun hentBrevDataFerdigstillingBarnepensjon(request: BrevDataFerdigstillingRequest) =
        coroutineScope {
            val behandlingId = requireNotNull(request.behandlingId)
            val beregning =
                if (hentUtfall(request) == VilkaarsvurderingUtfall.IKKE_OPPFYLT) {
                    null
                } else {
                    hentBeregning(behandlingId, request)
                }
            BarnepensjonVarsel(
                innhold = request.innholdMedVedlegg.innhold(),
                beregning = beregning,
                erUnder18Aar = request.soekerUnder18 ?: true,
                erBosattUtlandet = request.utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
            )
        }

    private suspend fun hentUtfall(request: BrevDataFerdigstillingRequest) =
        vilkaarsvurderingService.hentVilkaarsvurdering(request.behandlingId!!, request.bruker).resultat?.utfall

    private suspend fun hentBeregning(
        behandlingId: UUID,
        request: BrevDataFerdigstillingRequest,
    ): BarnepensjonBeregning =
        coroutineScope {
            val grunnbeloep = async { beregningService.hentGrunnbeloep(request.bruker) }
            val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, request.bruker) }
            val utbetalingsinfo =
                async {
                    beregningService.finnUtbetalingsinfo(
                        behandlingId,
                        YearMonth.now(),
                        // Virkningstidspunkt-feltet blir per no ikkje brukt i dette brevet.
                        // Pga gjenbruk av objekt etc er det ikkje trivielt å skrive oss bort frå det heller
                        request.bruker,
                        request.sakType,
                    )
                }
            barnepensjonBeregning(
                innhold = request.innholdMedVedlegg,
                avdoede = request.avdoede,
                utbetalingsinfo = utbetalingsinfo.await(),
                grunnbeloep = grunnbeloep.await(),
                beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo.await()),
                trygdetid = requireNotNull(trygdetid.await()),
                erForeldreloes = request.erForeldreloes,
            )
        }
}
