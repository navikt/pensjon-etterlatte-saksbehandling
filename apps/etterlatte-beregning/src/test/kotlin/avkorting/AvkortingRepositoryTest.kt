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
internal class AvkortingRepositoryTest(ds: DataSource) {
    private val avkortingRepository = AvkortingRepository(ds)

    @Test
    fun `skal returnere null hvis mangler avkorting`() {
        avkortingRepository.hentAvkorting(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal lagre og oppdatere avkorting`() {
        val behandlingId: UUID = UUID.randomUUID()
        val grunnlag = avkortinggrunnlag()
        val aarsoppgjoer =
            Aarsoppgjoer(
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

        avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer),
            ),
        )
        val endretGrunnlag = aarsoppgjoer.inntektsavkorting[0].grunnlag.copy(spesifikasjon = "Endret")
        val endretAvkortingsperioder =
            aarsoppgjoer.inntektsavkorting[0].avkortingsperioder.map { it.copy(avkorting = 333) }
        val endretAvkortetYtelse =
            aarsoppgjoer.inntektsavkorting[0].avkortetYtelseForventetInntekt.map { it.copy(avkortingsbeloep = 444) }

        val endretYtelseFoerAvkorting = listOf(aarsoppgjoer.ytelseFoerAvkorting[0].copy(beregning = 333))
        val endretInntektsavkorting =
            listOf(
                Inntektsavkorting(
                    grunnlag = endretGrunnlag,
                    avkortingsperioder = endretAvkortingsperioder,
                    avkortetYtelseForventetInntekt = endretAvkortetYtelse,
                ),
            )
        val endretAvkortetYtelseAar = aarsoppgjoer.avkortetYtelseAar.map { it.copy(avkortingsbeloep = 444) }

        avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            ytelseFoerAvkorting = endretYtelseFoerAvkorting,
                            inntektsavkorting = endretInntektsavkorting,
                            avkortetYtelseAar = endretAvkortetYtelseAar,
                        ),
                    ),
            ),
        )
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)

        with(avkorting!!.aarsoppgjoer.single()) {
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe endretYtelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 1
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe endretGrunnlag
                    avkorting.avkortingsperioder shouldBe endretAvkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe endretAvkortetYtelse
                }
            }
            avkortetYtelseAar.asClue {
                it.size shouldBe 1
                it shouldBe endretAvkortetYtelseAar
            }
        }
    }
}
