package avkorting

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingValider
import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
import no.nav.etterlatte.avkorting.FoersteRevurderingSenereEnnJanuar
import no.nav.etterlatte.avkorting.HarFratrekkInnAarForFulltAar
import no.nav.etterlatte.avkorting.InntektForTidligereAar
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.etteroppgjoer
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import java.time.YearMonth

class AvkortingValiderTest {
    @Test
    fun `Skal kunne endre inntekt tidligere aar hvis aarsoppgjoer er aarsoppgjoerloepende`() {
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

        val inntektMedFratrekk =
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 12),
            )

        validerInntekt(inntektMedFratrekk, avkorting, false, naa = YearMonth.of(2025, 1))
    }

    @Test
    fun `Skal ikke kunne endre inntekt tidligere aar hvis aarsoppgjoer er etteroppgjoer`() {
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        etteroppgjoer(aar = 2024),
                    ),
            )

        val inntektMedFratrekk =
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 12),
            )

        assertThrows<InntektForTidligereAar> {
            validerInntekt(inntektMedFratrekk, avkorting, false, naa = YearMonth.of(2025, 1))
        }
    }

    @Test
    fun `Skal kunne endre inntekt tidligere aar hvis det gjenaapnes etter opphoer`() {
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
                                            innvilgaMaaneder = 10,
                                            periode =
                                                Periode(
                                                    fom = YearMonth.of(2024, 2),
                                                    tom = YearMonth.of(2024, 11),
                                                ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )

        val inntektMedFratrekk =
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 12),
            )
        validerInntekt(inntektMedFratrekk, avkorting, false, naa = YearMonth.of(2025, 1))
    }

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
                    inntektTom = 100000,
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    inntektUtlandTom = 100000,
                    spesifikasjon = "asdf",
                    fom = YearMonth.of(2025, 2),
                )
            validerInntekt(inntektMedFratrekk, avkorting, false)
        }
    }

    @Test
    fun `Skal kunne angi ny inntekt gjeldende ifra tidligere enn forrige oppgitte`() {
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

        val inntektMedFratrekk =
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 1),
            )
        validerInntekt(inntektMedFratrekk, avkorting, false)
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
            validerInntekt(utenFratrekk, avkorting, true, naa = YearMonth.of(2024, 1))

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekk =
                    inntektDto(
                        fratrekkInnAar = 1,
                        fratrekkInnAarUtland = 0,
                        fom = YearMonth.of(2024, 1),
                    )
                validerInntekt(inntektMedFratrekk, avkorting, true, naa = YearMonth.of(2024, 1))
            }

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekkUtland =
                    inntektDto(
                        fratrekkInnAar = 0,
                        fratrekkInnAarUtland = 1,
                        fom = YearMonth.of(2024, 1),
                    )

                validerInntekt(inntektMedFratrekkUtland, avkorting, true, naa = YearMonth.of(2024, 1))
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
                                inntektsavkorting = emptyList(),
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
            inntektTom = 100000,
            fratrekkInnAar = fratrekkInnAar,
            fratrekkInnAarUtland = fratrekkInnAarUtland,
            inntektUtlandTom = 100000,
            spesifikasjon = "asdf",
            fom = fom,
        )
    }

    @Test
    fun `Førstegangsbehandling i gyldig tilstand gir ikke valideringsfeil`() {
        validerInntekt(
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 5000,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 2),
            ),
            Avkorting(),
            true,
            naa = YearMonth.of(2024, 1),
        )
    }

    @Test
    fun `Revurdering i gyldig tilstand gir ikke valideringsfeil`() {
        validerInntekt(
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = YearMonth.of(2024, 1),
            ),
            avkorting(),
            false,
            naa = YearMonth.of(2024, 1),
        )
    }

    @Test
    fun `påkrevde inntekter henter nødvendige inntekter med beregning over to år`() {
        val avkorting = Avkorting()
        val beregning =
            beregning(
                beregninger =
                    listOf(
                        beregningsperiode(
                            datoFOM = YearMonth.of(2024, Month.JULY),
                            datoTOM = YearMonth.of(2025, Month.APRIL),
                        ),
                        beregningsperiode(datoFOM = YearMonth.of(2025, Month.MAY)),
                    ),
            )
        val krav =
            AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                avkorting,
                beregning,
                BehandlingType.FØRSTEGANGSBEHANDLING,
            )
        krav shouldContainExactly listOf(2024, 2025)
    }
}
