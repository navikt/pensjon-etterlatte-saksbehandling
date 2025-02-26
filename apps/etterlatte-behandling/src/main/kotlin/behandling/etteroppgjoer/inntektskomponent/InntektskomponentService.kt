package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

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
) {
    companion object {
        fun stub(
            aar: Int = 2024,
            aarsinntekt: Int = 300000,
        ) = AInntekt(
            aar = 2024,
            // inntektsmaaneder = listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").map {
            inntektsmaaneder =
                listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).map {
                    AInntektMaaned(
                        // maaned = "$aar-$it",
                        maaned = YearMonth.of(aar, it),
                        inntekter =
                            listOf(
                                Inntekt(
                                    beloep = BigDecimal(aarsinntekt / 12),
                                    beskrivelse = "En månedsinntekt",
                                ),
                            ),
                        summertBeloep = BigDecimal(aarsinntekt / 12),
                    )
                },
        )
    }
}

data class AInntektMaaned(
    val maaned: YearMonth,
    val inntekter: List<Inntekt>,
    val summertBeloep: BigDecimal,
)

data class Inntekt(
    val beloep: BigDecimal,
    val beskrivelse: String,
)
