package no.nav.etterlatte.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.inntektsavkorting
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class AvkortingMapperTest {
    private val inntektFraMars24 =
        avkortinggrunnlag(
            innvilgaMaaneder = 10,
            periode =
                Periode(
                    fom = YearMonth.of(2024, Month.MARCH),
                    tom = YearMonth.of(2024, Month.JULY),
                ),
            inntektTom = 300000,
        )
    private val inntektFraAug24 =
        avkortinggrunnlag(
            innvilgaMaaneder = 10,
            periode =
                Periode(
                    fom = YearMonth.of(2024, Month.AUGUST),
                    tom = null,
                ),
            inntektTom = 350000,
        )
    private val inntektFraJan25 =
        avkortinggrunnlag(
            innvilgaMaaneder = 12,
            periode =
                Periode(
                    fom = YearMonth.of(2025, Month.JANUARY),
                    tom = null,
                ),
            inntektTom = 400000,
        )

    val avkorting =
        Avkorting(
            aarsoppgjoer =
                listOf(
                    AarsoppgjoerLoepende(
                        id = UUID.randomUUID(),
                        aar = 2024,
                        fom = YearMonth.of(2024, Month.MARCH),
                        inntektsavkorting =
                            listOf(
                                Inntektsavkorting(grunnlag = inntektFraMars24),
                                Inntektsavkorting(grunnlag = inntektFraAug24),
                            ),
                        avkortetYtelse =
                            listOf(
                                avkortetYtelse(
                                    periode =
                                        Periode(
                                            fom = YearMonth.of(2024, Month.MARCH),
                                            tom = YearMonth.of(2024, Month.APRIL),
                                        ),
                                ),
                                avkortetYtelse(
                                    periode =
                                        Periode(
                                            fom = YearMonth.of(2024, Month.MAY),
                                            tom = YearMonth.of(2024, Month.JULY),
                                        ),
                                ),
                                avkortetYtelse(
                                    periode = Periode(fom = YearMonth.of(2024, Month.AUGUST), tom = null),
                                ),
                            ),
                    ),
                    AarsoppgjoerLoepende(
                        id = UUID.randomUUID(),
                        aar = 2025,
                        fom = YearMonth.of(2025, Month.JANUARY),
                        inntektsavkorting =
                            listOf(
                                Inntektsavkorting(
                                    grunnlag = inntektFraJan25,
                                ),
                            ),
                        avkortetYtelse =
                            listOf(
                                avkortetYtelse(
                                    periode =
                                        Periode(
                                            fom = YearMonth.of(2025, Month.JANUARY),
                                            tom = null,
                                        ),
                                ),
                            ),
                    ),
                ),
        )

    @Test
    fun `fyller ut alle felter til avkortingsgrunnlag`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, 1)),
            )

        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false).avkortingGrunnlag.first().asClue {
            it.fom shouldBe inntektFraJan25.periode.fom
            it.tom shouldBe inntektFraJan25.periode.tom
            it.inntektTom shouldBe inntektFraJan25.inntektTom
            it.fratrekkInnAar shouldBe inntektFraJan25.fratrekkInnAar
            it.inntektUtlandTom shouldBe inntektFraJan25.inntektUtlandTom
            it.fratrekkInnAarUtland shouldBe inntektFraJan25.fratrekkInnAarUtland
            it.spesifikasjon shouldBe inntektFraJan25.spesifikasjon
            it.innvilgaMaaneder shouldBe 12
        }
    }

    @Test
    fun `fyller ut alle avkortingsgrunnlag i rekkefølge nyligste først`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 3)),
            )
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false).asClue {
            it.avkortingGrunnlag.size shouldBe 3
            it.avkortingGrunnlag[0].fom shouldBe inntektFraJan25.periode.fom
            it.avkortingGrunnlag[0].inntektTom shouldBe inntektFraJan25.inntektTom
            it.avkortingGrunnlag[1].fom shouldBe inntektFraAug24.periode.fom
            it.avkortingGrunnlag[1].inntektTom shouldBe inntektFraAug24.inntektTom
            it.avkortingGrunnlag[2].fom shouldBe inntektFraMars24.periode.fom
            it.avkortingGrunnlag[2].inntektTom shouldBe inntektFraMars24.inntektTom
        }
    }

    @Test
    fun `fyller ut alle perioder med avkortet ytelse`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.JANUARY)),
            )
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false).asClue {
            it.avkortetYtelse.size shouldBe 4

            it.avkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto()
            it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
            it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            it.avkortetYtelse[3] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
        }
    }

    @Test
    fun `fyller ut avkortet ytelse fra virkningstidspunkt`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.MAY)),
            )
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false).asClue {
            it.avkortetYtelse.size shouldBe 3

            it.avkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
            it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
        }
    }

    @Test
    fun `kutter periode hvis virkningstidspunkt begynner midt i periode `() {
        AvkortingMapper
            .avkortingForFrontend(
                avkorting,
                behandling(
                    status = BehandlingStatus.AVKORTET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.APRIL)),
                ),
                false,
            ).asClue {
                it.avkortetYtelse.size shouldBe 4
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.APRIL)
                    tom shouldBe YearMonth.of(2024, Month.APRIL)
                }
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            }

        AvkortingMapper
            .avkortingForFrontend(
                avkorting,
                behandling(
                    status = BehandlingStatus.AVKORTET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.JUNE)),
                ),
                false,
            ).asClue {
                it.avkortetYtelse.size shouldBe 3
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.JUNE)
                    tom shouldBe YearMonth.of(2024, Month.JULY)
                }
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            }

        AvkortingMapper
            .avkortingForFrontend(
                avkorting,
                behandling(
                    status = BehandlingStatus.AVKORTET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.SEPTEMBER)),
                ),
                false,
            ).asClue {
                it.avkortetYtelse.size shouldBe 2
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.SEPTEMBER)
                    tom shouldBe null
                }
            }

        AvkortingMapper
            .avkortingForFrontend(
                avkorting,
                behandling(
                    status = BehandlingStatus.AVKORTET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, Month.JANUARY)),
                ),
                false,
            ).asClue {
                it.avkortetYtelse.size shouldBe 1
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2025, Month.JANUARY)
                    tom shouldBe null
                }
            }
    }

    @Test
    fun `fyller ut tidligereAvkortetYtelse hvis finnes`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.MAY)),
            )
        val forrige = avkorting.copy()
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false, forrige).asClue {
            it.tidligereAvkortetYtelse.size shouldBe 4

            it.tidligereAvkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto()
            it.tidligereAvkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
            it.tidligereAvkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()

            it.tidligereAvkortetYtelse[3] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
        }
    }

    @Test
    fun `fyller ikke ut tidligereAvkortetYtelse hvis status iverksatt `() {
        val behandling =
            behandling(
                status = BehandlingStatus.IVERKSATT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.MAY)),
            )
        val forrige = avkorting.copy()
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false, forrige).asClue {
            it.tidligereAvkortetYtelse.size shouldBe 0
        }
    }

    @Test
    fun `legger ikke til redigerbar inntekt hvis ikke finnes`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.APRIL)),
            )
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, false).asClue {
            it.redigerbarForventetInntekt shouldBe null
            it.redigerbarForventetInntektNesteAar shouldBe null
        }
    }

    @Test
    fun `legger til redigerbar inntekt hvis finnes`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, Month.JANUARY)),
            )
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, true).asClue {
            it.redigerbarForventetInntekt shouldBe
                avkorting.aarsoppgjoer
                    .last()
                    .inntektsavkorting()
                    .single()
                    .grunnlag
                    .toDto()
            it.redigerbarForventetInntektNesteAar shouldBe null
        }
    }

    @Test
    fun `legger til redigerbar inntekt neste aar hvis finnes`() {
        val behandling =
            behandling(
                status = BehandlingStatus.AVKORTET,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.AUGUST)),
            )
        AvkortingMapper.avkortingForFrontend(avkorting, behandling, true).asClue {
            it.redigerbarForventetInntekt shouldBe
                avkorting.aarsoppgjoer
                    .first()
                    .inntektsavkorting()
                    .last()
                    .grunnlag
                    .toDto()
            it.redigerbarForventetInntektNesteAar shouldBe
                avkorting.aarsoppgjoer
                    .last()
                    .inntektsavkorting()
                    .single()
                    .grunnlag
                    .toDto()
        }
    }
}
