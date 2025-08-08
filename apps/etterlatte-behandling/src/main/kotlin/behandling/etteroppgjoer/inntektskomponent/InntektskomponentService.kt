package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.AInntektMaaned
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.Inntekt
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth

class InntektskomponentService(
    val klient: InntektskomponentKlient,
    val featureToggleService: FeatureToggleService,
) {
    suspend fun hentInntektFraAInntekt(
        personident: String,
        aar: Int,
    ): AInntekt {
        val skalStubbe = featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_STUB_INNTEKT, false)
        if (skalStubbe) {
            return AInntekt.stub()
        }

        val responsFraInntekt = hentInntekt(personident, aar)

        val inntektsmaaneder =
            responsFraInntekt.data.map { inntektsinfoMaaned ->
                val inntekterIMaaned =
                    inntektsinfoMaaned.inntektListe.map {
                        Inntekt(
                            beloep = it.beloep,
                            beskrivelse = it.beskrivelse,
                        )
                    }
                AInntektMaaned(
                    maaned = inntektsinfoMaaned.maaned,
                    inntekter = inntekterIMaaned,
                    summertBeloep = inntekterIMaaned.sumOf { it.beloep },
                )
            }

        return AInntekt(
            aar = aar,
            inntektsmaaneder = inntektsmaaneder,
        )
    }

    private suspend fun hentInntekt(
        personident: String,
        aar: Int,
    ) = klient.hentInntekt(
        personident = personident,
        maanedFom = YearMonth.of(aar, 1),
        maanedTom = YearMonth.of(aar, 12),
    )

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
