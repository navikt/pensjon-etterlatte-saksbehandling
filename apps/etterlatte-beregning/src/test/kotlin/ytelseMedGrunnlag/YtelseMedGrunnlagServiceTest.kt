package no.nav.etterlatte.beregning.regler.ytelseMedGrunnlag

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.ytelseMedGrunnlag.YtelseMedGrunnlagService
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class YtelseMedGrunnlagServiceTest {

    private val avkortingRepository = mockk<AvkortingRepository>()
    private val service = YtelseMedGrunnlagService(avkortingRepository)

    @Test
    fun `returnerer null hvis avkorting ikke finnes`() {
        every { avkortingRepository.hentAvkorting(any()) } returns null
        service.hentYtelseMedGrunnlag(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `skal hente ytelse oppdelt i perioder med alle grunnlag`() {
        val behandlingsId = UUID.randomUUID()
        every { avkortingRepository.hentAvkorting(behandlingsId) } returns avkorting(
            avkortingGrunnlag = listOf(
                avkortinggrunnlag(
                    periode = Periode(fom = YearMonth.of(2023, 2), tom = YearMonth.of(2023, 3)),
                    aarsinntekt = 300000,
                    fratrekkInnAar = 25000

                ),
                avkortinggrunnlag(
                    periode = Periode(fom = YearMonth.of(2023, 4), tom = null),
                    aarsinntekt = 350000,
                    fratrekkInnAar = 25000
                )
            ),
            avkortetYtelse = listOf(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2023, 2), tom = YearMonth.of(2023, 3)),
                    ytelseEtterAvkorting = 15000,
                    ytelseFoerAvkorting = 20000,
                    avkortingsbeloep = 5000

                ),
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2023, 4), tom = YearMonth.of(2023, 5)),
                    ytelseEtterAvkorting = 17000,
                    ytelseFoerAvkorting = 20000,
                    avkortingsbeloep = 3000
                ),
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2023, 6), tom = null),
                    ytelseEtterAvkorting = 17000,
                    ytelseFoerAvkorting = 21000,
                    avkortingsbeloep = 4000
                )
            )
        )

        val ytelse = service.hentYtelseMedGrunnlag(behandlingsId)

        with(ytelse!!.perioder[0]) {
            periode shouldBe Periode(fom = YearMonth.of(2023, 2), tom = YearMonth.of(2023, 3))
            ytelseEtterAvkorting shouldBe 15000
            ytelseFoerAvkorting shouldBe 20000
            avkortingsbeloep shouldBe 5000
            aarsinntekt shouldBe 300000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 111477
            grunnbelopMnd shouldBe 9290
        }
        with(ytelse.perioder[1]) {
            periode shouldBe Periode(fom = YearMonth.of(2023, 4), tom = YearMonth.of(2023, 5))
            ytelseEtterAvkorting shouldBe 17000
            ytelseFoerAvkorting shouldBe 20000
            avkortingsbeloep shouldBe 3000
            aarsinntekt shouldBe 350000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 111477
            grunnbelopMnd shouldBe 9290
        }
        with(ytelse.perioder[2]) {
            periode shouldBe Periode(fom = YearMonth.of(2023, 6), tom = null)
            ytelseEtterAvkorting shouldBe 17000
            ytelseFoerAvkorting shouldBe 21000
            avkortingsbeloep shouldBe 4000
            aarsinntekt shouldBe 350000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 118620
            grunnbelopMnd shouldBe 9885
        }
    }
}