package avkorting

import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
import no.nav.etterlatte.avkorting.ErFulltAar
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData.Companion.virkningstidsunkt
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class AvkortingValiderTest {
    @Test
    fun `Skal få valideringsfeil hvis ny inntekt gjelder ifra tidligere enn forrige oppgitte`() {
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            forventaInnvilgaMaaneder = 12,
                            aar = 2024,
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(
                                        avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2024, 1), tom = null)),
                                    ),
                                    Inntektsavkorting(
                                        avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2024, 2), tom = null)),
                                    ),
                                ),
                        ),
                    ),
            )
        val behandling =
            behandling(
                virkningstidspunkt = virkningstidsunkt(YearMonth.of(2024, 1)),
                behandlingType = BehandlingType.REVURDERING,
            )

        assertThrows<IkkeTillattException> {
            val inntektMedFratrekk =
                AvkortingGrunnlagLagreDto(
                    aarsinntekt = 100000,
                    fratrekkInnAar = 0,
                    fratrekkInnAarUtland = 0,
                    inntektUtland = 100000,
                    spesifikasjon = "asdf",
                )
            validerInntekt(inntektMedFratrekk, avkorting, behandling)
        }
    }

    @Nested
    inner class `Skal få valideringsfeil hvis det er angitt fratrekk inn år i et fullt år` {
        @Test
        fun `Førstegangsbehandling fra januar`() {
            val avkorting = avkorting()
            val behandling =
                behandling(
                    virkningstidspunkt = virkningstidsunkt(YearMonth.of(2024, 1)),
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                )

            val utenFratrekk = inntektDto(fratrekkInnAar = 0, fratrekkInnAarUtland = 0)
            validerInntekt(utenFratrekk, avkorting, behandling)

            assertThrows<ErFulltAar> {
                val inntektMedFratrekk = inntektDto(fratrekkInnAar = 1, fratrekkInnAarUtland = 0)
                validerInntekt(inntektMedFratrekk, avkorting, behandling)
            }

            assertThrows<ErFulltAar> {
                val inntektMedFratrekkUtland = inntektDto(fratrekkInnAar = 0, fratrekkInnAarUtland = 1)
                validerInntekt(inntektMedFratrekkUtland, avkorting, behandling)
            }
        }

        @Test
        fun `Revurdering i et inn år med 12 innvilgede måneder`() {
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(
                                forventaInnvilgaMaaneder = 12,
                                aar = 2024,
                                inntektsavkorting =
                                    listOf(
                                        Inntektsavkorting(
                                            avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2024, 1), tom = null)),
                                        ),
                                    ),
                            ),
                        ),
                )
            val behandling =
                behandling(
                    virkningstidspunkt = virkningstidsunkt(YearMonth.of(2024, 3)),
                    behandlingType = BehandlingType.REVURDERING,
                )

            val utenFratrekk = inntektDto(fratrekkInnAar = 0, fratrekkInnAarUtland = 0)
            validerInntekt(utenFratrekk, avkorting, behandling)

            assertThrows<ErFulltAar> {
                val inntektMedFratrekk = inntektDto(fratrekkInnAar = 1, fratrekkInnAarUtland = 0)
                validerInntekt(inntektMedFratrekk, avkorting, behandling)
            }

            assertThrows<ErFulltAar> {
                val inntektMedFratrekkUtland = inntektDto(fratrekkInnAar = 0, fratrekkInnAarUtland = 1)
                validerInntekt(inntektMedFratrekkUtland, avkorting, behandling)
            }
        }

        @Test
        fun `Revurdering inn i nytt år`() {
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(
                                forventaInnvilgaMaaneder = 6,
                                aar = 2024,
                                inntektsavkorting =
                                    listOf(
                                        Inntektsavkorting(
                                            avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2024, 7), tom = null)),
                                        ),
                                    ),
                            ),
                        ),
                )
            val behandling =
                behandling(
                    virkningstidspunkt = virkningstidsunkt(YearMonth.of(2025, 1)),
                    behandlingType = BehandlingType.REVURDERING,
                )

            val utenFratrekk = inntektDto(fratrekkInnAar = 0, fratrekkInnAarUtland = 0)
            validerInntekt(utenFratrekk, avkorting, behandling)

            assertThrows<ErFulltAar> {
                val inntektMedFratrekk = inntektDto(fratrekkInnAar = 1, fratrekkInnAarUtland = 0)
                validerInntekt(inntektMedFratrekk, avkorting, behandling)
            }

            assertThrows<ErFulltAar> {
                val inntektMedFratrekkUtland = inntektDto(fratrekkInnAar = 0, fratrekkInnAarUtland = 1)
                validerInntekt(inntektMedFratrekkUtland, avkorting, behandling)
            }
        }

        private fun inntektDto(
            fratrekkInnAar: Int,
            fratrekkInnAarUtland: Int,
        ) = AvkortingGrunnlagLagreDto(
            aarsinntekt = 100000,
            fratrekkInnAar = fratrekkInnAar,
            fratrekkInnAarUtland = fratrekkInnAarUtland,
            inntektUtland = 100000,
            spesifikasjon = "asdf",
        )
    }

    @Test
    fun `Gyldig tilstand gir ikke valideringsfeil`() {
        validerInntekt(
            AvkortingGrunnlagLagreDto(
                aarsinntekt = 100000,
                fratrekkInnAar = 0,
                fratrekkInnAarUtland = 0,
                inntektUtland = 100000,
                spesifikasjon = "asdf",
            ),
            avkorting(),
            behandling(),
        )
    }
}
