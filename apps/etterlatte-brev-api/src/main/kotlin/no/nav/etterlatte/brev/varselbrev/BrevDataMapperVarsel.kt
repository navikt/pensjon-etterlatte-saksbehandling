package no.nav.etterlatte.brev.varselbrev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.BrevDatafetcher
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarsel
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregning
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregningsperioder
import no.nav.etterlatte.libs.common.behandling.SakType

class BrevDataMapperVarsel(private val brevdataFacade: BrevdataFacade) {
    suspend fun hentBrevDataFerdigstilling(request: BrevDataFerdigstillingRequest) =
        coroutineScope {
            when (request.generellBrevData.sak.sakType) {
                SakType.BARNEPENSJON -> hentBrevDataFerdigstillingBarnepensjon(request)
                SakType.OMSTILLINGSSTOENAD -> ManueltBrevData(request.innholdMedVedlegg.innhold())
            }
        }

    private suspend fun hentBrevDataFerdigstillingBarnepensjon(it: BrevDataFerdigstillingRequest) =
        coroutineScope {
            val fetcher = BrevDatafetcher(brevdataFacade, it.bruker, it.generellBrevData)
            val grunnbeloep = async { fetcher.hentGrunnbeloep() }
            val trygdetid = async { fetcher.hentTrygdetid() }
            val utbetalingsinfo = async { fetcher.hentUtbetaling() }
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
