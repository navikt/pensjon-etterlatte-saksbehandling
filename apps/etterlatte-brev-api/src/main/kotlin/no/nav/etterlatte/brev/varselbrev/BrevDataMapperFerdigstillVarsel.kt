package no.nav.etterlatte.brev.varselbrev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.EtterlatteBrevKode
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarsel
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregning
import no.nav.etterlatte.brev.model.bp.barnepensjonBeregningsperioder
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAktivitetspliktVarsel
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.erBosattUtland
import java.time.YearMonth

class BrevDataMapperFerdigstillVarsel(
    private val beregningService: BeregningService,
    private val trygdetidService: TrygdetidService,
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
            if (request.revurderingaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                OmstillingsstoenadAktivitetspliktVarsel(
                    request.innholdMedVedlegg.innhold(),
                )
            } else {
                ManueltBrevMedTittelData(
                    request.innholdMedVedlegg.innhold(),
                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL.tittel,
                )
            }
        }

    private suspend fun hentBrevDataFerdigstillingBarnepensjon(request: BrevDataFerdigstillingRequest) =
        coroutineScope {
            val behandlingId = requireNotNull(request.behandlingId)
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
            BarnepensjonVarsel(
                innhold = request.innholdMedVedlegg.innhold(),
                beregning =
                    barnepensjonBeregning(
                        innhold = request.innholdMedVedlegg,
                        avdoede = request.avdoede,
                        utbetalingsinfo = utbetalingsinfo.await(),
                        grunnbeloep = grunnbeloep.await(),
                        beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo.await()),
                        trygdetid = requireNotNull(trygdetid.await()),
                        erForeldreloes = request.erForeldreloes,
                    ),
                erUnder18Aar = request.soeker.under18 ?: true,
                erBosattUtlandet = request.utlandstilknytningType?.erBosattUtland() ?: false,
            )
        }
}
