package avkorting

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingValider
import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekter
import no.nav.etterlatte.avkorting.HarFratrekkInnAarForFulltAar
import no.nav.etterlatte.avkorting.InntektForTidligereAar
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.NyeAarMedInntektMaaStarteIJanuar
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.etteroppgjoer
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import java.time.YearMonth
import java.util.UUID

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
        validerInntekter(
            behandling(BehandlingType.REVURDERING, virk = inntektMedFratrekk.fom),
            beregning(
                beregningsperiode(datoFOM = inntektMedFratrekk.fom),
            ),
            avkorting,
            listOf(inntektMedFratrekk),
            naa = YearMonth.of(2024, 12),
        )
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

        val fom = YearMonth.of(2024, 12)

        val inntektMedFratrekk =
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = fom,
            )

        assertThrows<InntektForTidligereAar> {
            validerInntekter(
                behandling(BehandlingType.REVURDERING),
                beregning(beregningsperiode(datoFOM = fom)),
                avkorting,
                listOf(inntektMedFratrekk),
                naa = fom,
            )
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
        val fom = YearMonth.of(2024, 12)
        val inntektMedFratrekk =
            AvkortingGrunnlagLagreDto(
                inntektTom = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtlandTom = 100000,
                spesifikasjon = "asdf",
                fom = fom,
            )
        validerInntekter(
            behandling(BehandlingType.REVURDERING),
            beregning(beregningsperiode(datoFOM = fom)),
            avkorting,
            listOf(inntektMedFratrekk),
            naa = fom,
        )
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
        val fom = YearMonth.of(2025, 2)

        assertThrows<NyeAarMedInntektMaaStarteIJanuar> {
            val inntektMedFratrekk =
                AvkortingGrunnlagLagreDto(
                    inntektTom = 100000,
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    inntektUtlandTom = 100000,
                    spesifikasjon = "asdf",
                    fom = fom,
                )
            validerInntekter(
                behandling(BehandlingType.REVURDERING),
                beregning(beregningsperiode(datoFOM = fom)),
                avkorting,
                listOf(inntektMedFratrekk),
                naa = fom,
            )
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
        validerInntekter(
            behandling(BehandlingType.REVURDERING),
            beregning(),
            avkorting,
            listOf(inntektMedFratrekk),
            naa = YearMonth.of(2024, 6),
        )
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
            validerInntekter(
                behandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, 1)),
                beregning(beregningsperiode(datoFOM = YearMonth.of(2024, 1))),
                avkorting,
                listOf(utenFratrekk),
                naa = YearMonth.of(2024, 3),
            )

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekk =
                    inntektDto(
                        fratrekkInnAar = 1,
                        fratrekkInnAarUtland = 0,
                        fom = YearMonth.of(2024, 1),
                    )
                validerInntekter(
                    behandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, 1)),
                    beregning(beregningsperiode(datoFOM = YearMonth.of(2024, 1))),
                    avkorting,
                    listOf(inntektMedFratrekk),
                    naa = YearMonth.of(2024, 3),
                )
            }

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekkUtland =
                    inntektDto(
                        fratrekkInnAar = 0,
                        fratrekkInnAarUtland = 1,
                        fom = YearMonth.of(2024, 1),
                    )

                validerInntekter(
                    behandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, 1)),
                    beregning(beregningsperiode(datoFOM = YearMonth.of(2024, 1))),
                    avkorting,
                    listOf(inntektMedFratrekkUtland),
                    naa = YearMonth.of(2024, 3),
                )
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
            validerInntekter(
                behandling = behandling(BehandlingType.REVURDERING),
                beregning = beregning(beregningsperiode(datoFOM = YearMonth.of(2025, 1))),
                eksisterendeAvkorting = avkorting,
                nyeGrunnlag = listOf(utenFratrekk),
            )

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekk =
                    inntektDto(
                        fratrekkInnAar = 1,
                        fratrekkInnAarUtland = 0,
                        fom = YearMonth.of(2025, 1),
                    )
                validerInntekter(
                    behandling(BehandlingType.REVURDERING),
                    beregning(),
                    avkorting,
                    listOf(inntektMedFratrekk),
                )
            }

            assertThrows<HarFratrekkInnAarForFulltAar> {
                val inntektMedFratrekkUtland =
                    inntektDto(
                        fratrekkInnAar = 0,
                        fratrekkInnAarUtland = 1,
                        fom = YearMonth.of(2025, 1),
                    )
                validerInntekter(
                    behandling(BehandlingType.REVURDERING),
                    beregning(),
                    avkorting,
                    listOf(inntektMedFratrekkUtland),
                )
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
        validerInntekter(
            eksisterendeAvkorting = Avkorting(aarsoppgjoer = listOf()),
            nyeGrunnlag =
                listOf(
                    AvkortingGrunnlagLagreDto(
                        inntektTom = 100000,
                        fratrekkInnAar = 5000,
                        fratrekkInnAarUtland = 0,
                        inntektUtlandTom = 100000,
                        spesifikasjon = "asdf",
                        fom = YearMonth.of(2024, 2),
                    ),
                ),
            behandling = behandling(BehandlingType.FØRSTEGANGSBEHANDLING, virk = YearMonth.of(2024, 2)),
            beregning = beregning(beregningsperiode(datoFOM = YearMonth.of(2024, 2))),
            naa = YearMonth.of(2024, 5),
        )
    }

    @Test
    fun `Revurdering i gyldig tilstand gir ikke valideringsfeil`() {
        validerInntekter(
            naa = YearMonth.of(2024, 1),
            eksisterendeAvkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            AarsoppgjoerLoepende(
                                id = UUID.randomUUID(),
                                aar = 2024,
                                fom = YearMonth.of(2024, 2),
                                ytelseFoerAvkorting = listOf(),
                                inntektsavkorting = listOf(),
                                avkortetYtelse = listOf(),
                            ),
                        ),
                ),
            nyeGrunnlag =
                listOf(
                    AvkortingGrunnlagLagreDto(
                        inntektTom = 100000,
                        fratrekkInnAar = 5000,
                        fratrekkInnAarUtland = 0,
                        inntektUtlandTom = 100000,
                        spesifikasjon = "asdf",
                        fom = YearMonth.of(2024, 2),
                    ),
                ),
            behandling = behandling(BehandlingType.REVURDERING, virk = YearMonth.of(2024, 2)),
            beregning = beregning(beregningsperiode(datoFOM = YearMonth.of(2024, 2))),
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
        krav shouldContainExactly listOf(2024, 2025, 2026)
    }

    private fun behandling(
        type: BehandlingType,
        virk: YearMonth? = YearMonth.of(2024, Month.APRIL),
        behandlingId: UUID = UUID.randomUUID(),
        sakId: SakId = SakId(1L),
    ): DetaljertBehandling =
        mockk {
            every { behandlingType } returns type
            every { id } returns behandlingId
            every { sak } returns sakId
            every { virkningstidspunkt } returns
                if (virk == null) {
                    null
                } else {
                    mockk {
                        every { dato } returns virk
                    }
                }
        }

    private fun beregning(periode: Beregningsperiode): Beregning =
        mockk {
            every { beregningsperioder } returns listOf(periode)
        }
}
