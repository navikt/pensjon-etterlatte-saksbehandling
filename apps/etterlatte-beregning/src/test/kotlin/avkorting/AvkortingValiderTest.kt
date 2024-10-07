package avkorting

import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
import no.nav.etterlatte.avkorting.FoersteRevurderingSenereEnnJanuar
import no.nav.etterlatte.avkorting.HarFratrekkInnAarForFulltAar
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.RevurderingHarEndretFratrekkInnAar
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class AvkortingValiderTest {
    @Test
    fun `Første revurdering i et nytt år må være fom januar`() {
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            aar = 2024,
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(
                                        avkortinggrunnlag(
                                            innvilgaMaaneder = 11,
                                            periode = Periode(fom = YearMonth.of(2024, 2), tom = null),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )

        assertThrows<FoersteRevurderingSenereEnnJanuar> {
            val inntektMedFratrekk =
                AvkortingGrunnlagLagreDto(
                    aarsinntekt = 100000,
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    inntektUtland = 100000,
                    spesifikasjon = "asdf",
                    fom = YearMonth.of(2025, 2),
                )
            validerInntekt(inntektMedFratrekk, avkorting, false)
        }
    }

    @Test
    fun `Skal få valideringsfeil hvis ny inntekt gjelder ifra tidligere enn forrige oppgitte`() {
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            aar = 2024,
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(
                                        avkortinggrunnlag(
                                            innvilgaMaaneder = 11,
                                            periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
                                        ),
                                    ),
                                    Inntektsavkorting(
                                        avkortinggrunnlag(
                                            innvilgaMaaneder = 11,
                                            periode = Periode(fom = YearMonth.of(2024, 2), tom = null),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )

        assertThrows<IkkeTillattException> {
            val inntektMedFratrekk =
                AvkortingGrunnlagLagreDto(
                    aarsinntekt = 100000,
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    inntektUtland = 100000,
                    spesifikasjon = "asdf",
                    fom = YearMonth.of(2024, 1),
                )
            validerInntekt(inntektMedFratrekk, avkorting, false)
        }
    }

    @Nested
    inner class `Skal få valideringsfeil hvis det er angitt eller endret fratrekk etter innvilgelse eller et fullt år` {
        @Test
        fun `Førstegangsbehandling fra januar`() {
            val avkorting = avkorting()

            val utenFratrekk =
                inntektDto(
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    fom = YearMonth.of(2024, 1),
                )
            validerInntekt(utenFratrekk, avkorting, true)

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekk =
                    inntektDto(
                        fratrekkInnAar = 1,
                        fratrekkInnAarUtland = 0,
                        fom = YearMonth.of(2024, 1),
                    )
                validerInntekt(inntektMedFratrekk, avkorting, true)
            }

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekkUtland =
                    inntektDto(
                        fratrekkInnAar = 0,
                        fratrekkInnAarUtland = 1,
                        fom = YearMonth.of(2024, 1),
                    )

                validerInntekt(inntektMedFratrekkUtland, avkorting, true)
            }
        }

        @Test
        fun `Revurdering med tidligere grunnlag samme år`() {
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(
                                aar = 2024,
                                inntektsavkorting =
                                    listOf(
                                        Inntektsavkorting(
                                            avkortinggrunnlag(
                                                innvilgaMaaneder = 12,
                                                periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
                                                fratrekkInnAar = 100000,
                                                fratrekkInnAarUtland = 50000,
                                            ),
                                        ),
                                    ),
                            ),
                        ),
                )

            val utenFratrekk =
                inntektDto(
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    fom = YearMonth.of(2024, 3),
                )
            validerInntekt(utenFratrekk, avkorting, false)

            assertThrows<RevurderingHarEndretFratrekkInnAar> {
                val inntektMedFratrekk =
                    inntektDto(
                        fratrekkInnAar = 100001,
                        fratrekkInnAarUtland = 50000,
                        fom = YearMonth.of(2024, 3),
                    )
                validerInntekt(inntektMedFratrekk, avkorting, false)
            }

            assertThrows<RevurderingHarEndretFratrekkInnAar> {
                val inntektMedFratrekkUtland =
                    inntektDto(
                        fratrekkInnAar = 100000,
                        fratrekkInnAarUtland = 50001,
                        fom = YearMonth.of(2024, 3),
                    )
                validerInntekt(inntektMedFratrekkUtland, avkorting, false)
            }
        }

        @Test
        fun `Revurdering nytt år`() {
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(
                                aar = 2025,
                                inntektsavkorting = listOf(),
                            ),
                        ),
                )

            val utenFratrekk =
                inntektDto(
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    fom = YearMonth.of(2025, 1),
                )
            validerInntekt(utenFratrekk, avkorting, false)

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekk =
                    inntektDto(
                        fratrekkInnAar = 1,
                        fratrekkInnAarUtland = 0,
                        fom = YearMonth.of(2025, 1),
                    )
                validerInntekt(inntektMedFratrekk, avkorting, false)
            }

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekkUtland =
                    inntektDto(
                        fratrekkInnAar = 0,
                        fratrekkInnAarUtland = 1,
                        fom = YearMonth.of(2025, 1),
                    )
                validerInntekt(inntektMedFratrekkUtland, avkorting, false)
            }
        }

        private fun inntektDto(
            fratrekkInnAar: Int,
            fratrekkInnAarUtland: Int,
            fom: YearMonth,
        ) = AvkortingGrunnlagLagreDto(
            aarsinntekt = 100000,
            fratrekkInnAar = fratrekkInnAar,
            fratrekkInnAarUtland = fratrekkInnAarUtland,
            inntektUtland = 100000,
            spesifikasjon = "asdf",
            fom = fom,
        )
    }

    @Test
    fun `Førstegangsbehandling i gyldig tilstand gir ikke valideringsfeil`() {
        validerInntekt(
            AvkortingGrunnlagLagreDto(
                aarsinntekt = 100000,
                fratrekkInnAar = 5000,
                fratrekkInnAarUtland = 0,
                inntektUtland = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 2),
            ),
            Avkorting(),
            true,
        )
    }

    @Test
    fun `Revurdering i gyldig tilstand gir ikke valideringsfeil`() {
        validerInntekt(
            AvkortingGrunnlagLagreDto(
                aarsinntekt = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtland = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 1),
            ),
            avkorting(),
            false,
        )
    }
}
