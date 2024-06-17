package no.nav.etterlatte.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvkortingRepositoryTest(
    ds: DataSource,
) {
    private val avkortingRepository = AvkortingRepository(ds)

    @Test
    fun `skal returnere null hvis mangler avkorting`() {
        avkortingRepository.hentAvkorting(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal lagre og oppdatere avkorting`() {
        val behandlingId: UUID = UUID.randomUUID()
        val aarsoppgjoer = nyAvkorting(2024, grunnlag = avkortinggrunnlag())

        avkortingRepository.lagreAvkorting(
            behandlingId,
            123L,
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer),
            ),
        )

        val endretAarsoppgjoer =
            aarsoppgjoer(
                ytelseFoerAvkorting = listOf(aarsoppgjoer.ytelseFoerAvkorting[0].copy(beregning = 333)),
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(
                            grunnlag = aarsoppgjoer.inntektsavkorting[0].grunnlag.copy(spesifikasjon = "Endret"),
                            avkortingsperioder = aarsoppgjoer.inntektsavkorting[0].avkortingsperioder.map { it.copy(avkorting = 333) },
                            avkortetYtelseForventetInntekt =
                                aarsoppgjoer.inntektsavkorting[0].avkortetYtelseForventetInntekt.map {
                                    it.copy(
                                        avkortingsbeloep = 444,
                                    )
                                },
                        ),
                    ),
                avkortetYtelseAar = aarsoppgjoer.avkortetYtelseAar.map { it.copy(avkortingsbeloep = 444) },
            )

        val nyttArsoppgjoer = nyAvkorting(2025, grunnlag = avkortinggrunnlag())

        avkortingRepository.lagreAvkorting(
            behandlingId,
            123L,
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
                it.size shouldBe 1
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe endretAarsoppgjoer.inntektsavkorting.single().grunnlag
                    avkorting.avkortingsperioder shouldBe endretAarsoppgjoer.inntektsavkorting.single().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        endretAarsoppgjoer.inntektsavkorting.single().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelseAar.asClue {
                it.size shouldBe 1
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
                it.size shouldBe 1
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe nyttArsoppgjoer.inntektsavkorting.single().grunnlag
                    avkorting.avkortingsperioder shouldBe nyttArsoppgjoer.inntektsavkorting.single().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        nyttArsoppgjoer.inntektsavkorting.single().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelseAar.asClue {
                it.size shouldBe 1
                it shouldBe nyttArsoppgjoer.avkortetYtelseAar
            }
        }
    }

    private fun nyAvkorting(
        aar: Int,
        forventaInnvilgaMaaneder: Int = 12,
        grunnlag: AvkortingGrunnlag,
    ) = Aarsoppgjoer(
        id = UUID.randomUUID(),
        aar = aar,
        forventaInnvilgaMaaneder = forventaInnvilgaMaaneder,
        ytelseFoerAvkorting = listOf(ytelseFoerAvkorting()),
        inntektsavkorting =
            listOf(
                Inntektsavkorting(
                    grunnlag = grunnlag,
                    avkortingsperioder = listOf(avkortingsperiode(inntektsgrunnlag = grunnlag.id)),
                    avkortetYtelseForventetInntekt =
                        listOf(
                            avkortetYtelse(
                                type = AvkortetYtelseType.FORVENTET_INNTEKT,
                                inntektsgrunnlag = grunnlag.id,
                            ),
                        ),
                ),
            ),
        avkortetYtelseAar = listOf(avkortetYtelse(type = AvkortetYtelseType.AARSOPPGJOER)),
    )
}
