package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth

class InntektskomponentService(
    val klient: InntektskomponentKlient,
    val featureToggleService: FeatureToggleService,
) {
    suspend fun hentSummerteInntekter(
        personident: String,
        aar: Int,
    ): SummerteInntekterAOrdningen {
        val bulkInntekter =
            klient.hentInntektFlereFilter(
                personident = personident,
                maanedFom = YearMonth.of(aar, 1),
                maanedTom = YearMonth.of(aar, 12),
                filter = InntektskomponentenFilter.entries,
            )

        val afpData =
            krevIkkeNull(bulkInntekter.bulk.find { it.filter == InntektskomponentenFilter.ETTEROPPGJOER_AFP.filter }) {
                "Skal ha data med filter ${InntektskomponentenFilter.ETTEROPPGJOER_AFP}"
            }
        val afpResultat = InntektskomponentBeregning.beregnInntekt(afpData, aar)

        val omsData =
            krevIkkeNull(bulkInntekter.bulk.find { it.filter == InntektskomponentenFilter.ETTEROPPGJOER_OMS.filter }) {
                "Skal ha resultat med filter ${InntektskomponentenFilter.ETTEROPPGJOER_OMS}"
            }
        val omsResultat = InntektskomponentBeregning.beregnInntekt(omsData, aar)

        val loennData =
            krevIkkeNull(bulkInntekter.bulk.find { it.filter == InntektskomponentenFilter.ETTEROPPGJOER_LOENN.filter }) {
                "Skal ha resultat med filter ${InntektskomponentenFilter.ETTEROPPGJOER_LOENN}"
            }
        val loennResultat = InntektskomponentBeregning.beregnInntekt(loennData, aar)

        return SummerteInntekterAOrdningen(
            afp = afpResultat.verdi,
            loenn = loennResultat.verdi,
            oms = omsResultat.verdi,
            tidspunktBeregnet = Tidspunkt(loennResultat.opprettet),
            regelresultat =
                mapOf(
                    InntektskomponentenFilter.ETTEROPPGJOER_AFP to objectMapper.valueToTree(afpResultat),
                    InntektskomponentenFilter.ETTEROPPGJOER_LOENN to objectMapper.valueToTree(loennResultat),
                    InntektskomponentenFilter.ETTEROPPGJOER_OMS to objectMapper.valueToTree(omsResultat),
                ),
        )
    }
}
