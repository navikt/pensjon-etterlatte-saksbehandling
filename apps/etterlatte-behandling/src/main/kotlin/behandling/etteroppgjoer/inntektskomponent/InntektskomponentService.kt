package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.AInntektMaaned
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.Inntekt
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
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
}
