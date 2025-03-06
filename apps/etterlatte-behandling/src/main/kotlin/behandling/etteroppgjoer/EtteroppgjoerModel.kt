package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class Etteroppgjoer(
    val id: UUID,
    val aar: Int,
    val status: EtteroppgjoerStatus,
)

enum class EtteroppgjoerStatus {
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_HENDELSE,
    UNDER_FORBEHANDLING,
}

data class SkatteoppgjoerHendelse(
    val id: UUID,
    val sekvensnummerSkatt: String,
)

// TODO falte ut behandling..
data class ForbehandlingDto(
    val behandling: EtteroppgjoerForbehandling,
    val opplysninger: EtteroppgjoerOpplysninger,
)

data class EtteroppgjoerForbehandling(
    val id: UUID,
    val hendelseId: UUID,
    val status: String, // TODO enum
    val sak: Sak,
    val aar: Int,
    val opprettet: Tidspunkt,
)

data class EtteroppgjoerOpplysninger(
    val skatt: PensjonsgivendeInntektFraSkatt,
    val ainntekt: AInntekt,
    val tidligereAvkorting: AvkortingDto,
)

data class PensjonsgivendeInntektFraSkatt(
    val inntektsaar: String,
    val inntekter: List<PensjonsgivendeInntekt>,
) {
    companion object {
        fun stub(
            aar: Int = 2024,
            aarsinntekt: Int = 300000,
        ) = PensjonsgivendeInntektFraSkatt(
            inntektsaar = "2024",
            inntekter =
                listOf(
                    PensjonsgivendeInntekt(
                        skatteordning = "FASTLAND",
                        loensinntekt = aarsinntekt,
                        naeringsinntekt = 0,
                        annet = 0,
                    ),
                ),
        )
    }
}

data class PensjonsgivendeInntekt(
    val skatteordning: String,
    val loensinntekt: Int,
    val naeringsinntekt: Int,
    val annet: Int,
)

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
