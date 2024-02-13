package no.nav.etterlatte.brev.varselbrev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.hentinformasjon.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarsel
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregning
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregningsperioder
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.YearMonth

class BrevDataMapperVarsel(
    private val beregningService: BeregningService,
    private val trygdetidService: TrygdetidService,
) {
    suspend fun hentBrevDataFerdigstilling(request: BrevDataFerdigstillingRequest) =
        coroutineScope {
            when (request.generellBrevData.sak.sakType) {
                SakType.BARNEPENSJON -> hentBrevDataFerdigstillingBarnepensjon(request)
                SakType.OMSTILLINGSSTOENAD -> ManueltBrevData(request.innholdMedVedlegg.innhold())
            }
        }

    private suspend fun hentBrevDataFerdigstillingBarnepensjon(it: BrevDataFerdigstillingRequest) =
        coroutineScope {
            val behandlingId = requireNotNull(it.generellBrevData.behandlingId)
            val grunnbeloep = async { beregningService.hentGrunnbeloep(it.bruker) }
            val trygdetid = async { trygdetidService.finnTrygdetid(behandlingId, it.bruker) }
            val utbetalingsinfo =
                async {
                    beregningService.finnUtbetalingsinfo(
                        behandlingId,
                        YearMonth.now(),
                        // Virkningstidspunkt-feltet blir per no ikkje brukt i dette brevet.
                        // Pga gjenbruk av objekt etc er det ikkje trivielt å skrive oss bort frå det heller
                        it.bruker,
                        it.generellBrevData.sak.sakType,
                    )
                }
            BarnepensjonVarsel(
                innhold = it.innholdMedVedlegg.innhold(),
                beregning =
                    barnepensjonBeregning(
                        innhold = it.innholdMedVedlegg,
                        utbetalingsinfo = utbetalingsinfo.await(),
                        grunnbeloep = grunnbeloep.await(),
                        beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo.await()),
                        trygdetid = requireNotNull(trygdetid.await()),
                    ),
                erUnder18Aar = it.generellBrevData.personerISak.soeker.under18 ?: true,
                erBosattUtlandet = it.generellBrevData.utlandstilknytning?.erBosattUtland() ?: false,
            )
        }
}
