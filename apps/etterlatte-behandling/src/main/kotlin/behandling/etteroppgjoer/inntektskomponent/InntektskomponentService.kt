package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.AInntektMaaned
import no.nav.etterlatte.behandling.etteroppgjoer.Inntekt
import java.time.YearMonth

class InntektskomponentService(
    val klient: InntektskomponentKlient,
) {
    suspend fun hentInntektFraAInntekt(
        personident: String,
        aar: Int,
    ): AInntekt {
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
