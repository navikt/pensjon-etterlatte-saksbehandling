package no.nav.etterlatte.avkorting.inntektskomponent

import java.math.BigDecimal
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

data class AInntekt(
    val aar: Int,
    val inntektsmaaneder: List<AInntektMaaned>,
)

data class AInntektMaaned(
    val maaned: String,
    val inntekter: List<Inntekt>,
    val summertBeloep: BigDecimal,
)

data class Inntekt(
    val beloep: BigDecimal,
    val beskrivelse: String,
)
