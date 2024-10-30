package no.nav.etterlatte.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
import no.nav.etterlatte.libs.common.beregning.AvkortingHarInntektForAarDto
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvkortingRepositoryTest(
    ds: DataSource,
) {
    private val avkortingRepository = AvkortingRepository(ds)

    @Test
    fun `skal returnere true hvis bruker har inntekt for aar`() {
        val behandlingId: UUID = UUID.randomUUID()
        val aarsoppgjoer = nyAvkorting(2024)
        val sakId = randomSakId()

        avkortingRepository.harSakInntektForAar(AvkortingHarInntektForAarDto(sakId, aarsoppgjoer.aar)) shouldBe false
        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer),
            ),
        )

        avkortingRepository.harSakInntektForAar(AvkortingHarInntektForAarDto(sakId, aarsoppgjoer.aar)) shouldBe true
    }

    @Test
    fun `skal returnere null hvis mangler avkorting`() {
        avkortingRepository.hentAvkorting(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal lagre og oppdatere avkorting`() {
        val behandlingId: UUID = UUID.randomUUID()
        val aarsoppgjoer = nyAvkorting(2024)
        val sakId = randomSakId()

        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer),
            ),
        )

        val endretAarsoppgjoer =
            aarsoppgjoer(
                ytelseFoerAvkorting = aarsoppgjoer.ytelseFoerAvkorting.map { it.copy(beregning = 333) },
                inntektsavkorting =
                    aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                        Inntektsavkorting(
                            grunnlag = inntektsavkorting.grunnlag.copy(spesifikasjon = "Endret"),
                            avkortingsperioder = inntektsavkorting.avkortingsperioder.map { it.copy(avkorting = 333) },
                            avkortetYtelseForventetInntekt =
                                inntektsavkorting.avkortetYtelseForventetInntekt.map {
                                    it.copy(
                                        avkortingsbeloep = 444,
                                    )
                                },
                        )
                    },
                avkortetYtelseAar = aarsoppgjoer.avkortetYtelseAar.map { it.copy(avkortingsbeloep = 444) },
            )

        val nyttArsoppgjoer = nyAvkorting(2025)

        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        endretAarsoppgjoer,
                        nyttArsoppgjoer,
                    ),
            ),
        )
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)

        with(avkorting!!.aarsoppgjoer[0]) {
            aar shouldBe 2024
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe endretAarsoppgjoer.ytelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 2
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe endretAarsoppgjoer.inntektsavkorting.first().grunnlag
                    avkorting.avkortingsperioder shouldBe endretAarsoppgjoer.inntektsavkorting.first().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        endretAarsoppgjoer.inntektsavkorting.first().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelseAar.asClue {
                it.size shouldBe 2
                it shouldBe endretAarsoppgjoer.avkortetYtelseAar
            }
        }

        with(avkorting.aarsoppgjoer[1]) {
            aar shouldBe 2025
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe nyttArsoppgjoer.ytelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 2
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe nyttArsoppgjoer.inntektsavkorting.first().grunnlag
                    avkorting.avkortingsperioder shouldBe nyttArsoppgjoer.inntektsavkorting.first().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        nyttArsoppgjoer.inntektsavkorting.first().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelseAar.asClue {
                it.size shouldBe 2
                it shouldBe nyttArsoppgjoer.avkortetYtelseAar
            }
        }
    }

    private fun nyAvkorting(
        aar: Int,
        forventaInnvilgaMaaneder: Int = 12,
    ): Aarsoppgjoer {
        val inntektEn =
            avkortinggrunnlag(
                innvilgaMaaneder = forventaInnvilgaMaaneder,
                periode = Periode(fom = YearMonth.of(aar, 1), tom = YearMonth.of(aar, 3)),
                overstyrtInnvilgaMaanederAarsak = OverstyrtInnvilgaMaanederAarsak.ANNEN,
                overstyrtInnvilgaMaanederBegrunnelse = "overstyrtInnvilgaMaanederBegrunnelse",
            )
        val inntektsavkortingEn =
            Inntektsavkorting(
                grunnlag = inntektEn,
                avkortingsperioder =
                    listOf(
                        avkortingsperiode(
                            inntektsgrunnlag = inntektEn.id,
                            fom = YearMonth.of(aar, 1),
                        ),
                    ),
                avkortetYtelseForventetInntekt =
                    listOf(
                        avkortetYtelse(
                            type = AvkortetYtelseType.FORVENTET_INNTEKT,
                            inntektsgrunnlag = inntektEn.id,
                            periode = Periode(fom = YearMonth.of(aar, 1), tom = null),
                        ),
                    ),
            )

        val inntektTo =
            avkortinggrunnlag(
                innvilgaMaaneder = forventaInnvilgaMaaneder,
                periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
            )
        val inntektsavkortingTo =
            Inntektsavkorting(
                grunnlag = inntektTo,
                avkortingsperioder =
                    listOf(
                        avkortingsperiode(
                            inntektsgrunnlag = inntektTo.id,
                            fom = YearMonth.of(aar, 4),
                        ),
                    ),
                avkortetYtelseForventetInntekt =
                    listOf(
                        avkortetYtelse(
                            type = AvkortetYtelseType.FORVENTET_INNTEKT,
                            inntektsgrunnlag = inntektTo.id,
                            periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
                        ),
                    ),
            )

        return Aarsoppgjoer(
            id = UUID.randomUUID(),
            aar = aar,
            fom = YearMonth.of(aar, 12),
            ytelseFoerAvkorting =
                listOf(
                    ytelseFoerAvkorting(
                        periode =
                            Periode(
                                fom = YearMonth.of(aar, 1),
                                tom = null,
                            ),
                    ),
                ),
            inntektsavkorting =
                listOf(
                    inntektsavkortingEn,
                    inntektsavkortingTo,
                ),
            avkortetYtelseAar =
                listOf(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode = Periode(fom = YearMonth.of(aar, 1), tom = YearMonth.of(aar, 3)),
                    ),
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
                    ),
                ),
        )
    }
}
