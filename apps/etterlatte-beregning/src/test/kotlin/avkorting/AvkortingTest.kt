package avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRegelkjoring
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class AvkortingTest {

    @BeforeEach
    fun beforeEach() {
        mockkObject(AvkortingRegelkjoring)
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Kopier avkorting lager ny avkorting med tidligere grunnlag med ny id`() {
        val grunnlag = listOf(avkortinggrunnlag(), avkortinggrunnlag())
        val kopi = avkorting(avkortingGrunnlag = grunnlag).kopierAvkorting()
        with(kopi) {
            with(avkortingGrunnlag) {
                size shouldBe 2
                get(0).id shouldNotBe grunnlag[0].id
                get(1).id shouldNotBe grunnlag[1].id
            }
            aarsoppgjoer shouldBe emptyList()
            avkortingsperioder shouldBe emptyList()
            avkortetYtelse shouldBe emptyList()
        }
    }

    @Test
    fun `Beregn avkorting med nytt grunnlag legger til og setter til og med paa periode til forrige loepende`() {
        val foersteGrunnlag = avkortinggrunnlag(
            periode = Periode(fom = YearMonth.of(2023, 1), tom = YearMonth.of(2023, 3))
        )
        val andreGrunnlag = avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2023, 4), tom = null))
        val nyttGrunnlag = avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2023, 8), tom = null))

        val avkorting = avkorting(
            avkortingGrunnlag = listOf(foersteGrunnlag, andreGrunnlag)
        )

        every { AvkortingRegelkjoring.beregnAarsoppgjoer(any(), any(), any()) } returns listOf(aarsoppgjoer())
        every { AvkortingRegelkjoring.beregnInntektsavkorting(any(), any()) } returns listOf(avkortingsperiode())
        every { AvkortingRegelkjoring.beregnAvkortetYtelse(any(), any(), any(), any()) } returns listOf(
            avkortetYtelse()
        )

        val beregnetAvkorting = avkorting.beregnAvkortingNyttEllerEndretGrunnlag(
            nyttGrunnlag,
            YearMonth.of(2023, 8),
            beregning()
        )

        with(beregnetAvkorting.avkortingGrunnlag) {
            size shouldBe 3
            get(0) shouldBe foersteGrunnlag
            get(1).id shouldBe andreGrunnlag.id
            get(1).periode.tom shouldBe YearMonth.of(2023, 7)
            get(2) shouldBe nyttGrunnlag
        }
    }

    @Test
    fun `Beregn avkorting med endret grunnlag oppdaterer eksisterende loepende grunnlag`() {
        val foersteGrunnlag = avkortinggrunnlag(
            periode = Periode(fom = YearMonth.of(2023, 1), tom = YearMonth.of(2023, 3))
        )
        val andreGrunnlag = avkortinggrunnlag(
            aarsinntekt = 1000000,
            periode = Periode(fom = YearMonth.of(2023, 4), tom = null)
        )
        val endretGrunnlag = andreGrunnlag.copy(aarsinntekt = 200000)

        val avkorting = avkorting(
            avkortingGrunnlag = listOf(foersteGrunnlag, andreGrunnlag)
        )

        every { AvkortingRegelkjoring.beregnAarsoppgjoer(any(), any(), any()) } returns listOf(aarsoppgjoer())
        every { AvkortingRegelkjoring.beregnInntektsavkorting(any(), any()) } returns listOf(avkortingsperiode())
        every { AvkortingRegelkjoring.beregnAvkortetYtelse(any(), any(), any(), any()) } returns listOf(
            avkortetYtelse()
        )

        val beregnetAvkorting = avkorting.beregnAvkortingNyttEllerEndretGrunnlag(
            endretGrunnlag,
            YearMonth.of(2023, 8),
            beregning()
        )

        with(beregnetAvkorting.avkortingGrunnlag) {
            size shouldBe 2
            get(0) shouldBe foersteGrunnlag
            get(1) shouldBe endretGrunnlag
        }
    }

    @Test
    fun `Beregn avkorting beregner avkortingsperioder og avkortet ytelse fra ny virk med restanse fra aarsoppgjoer`() {
        val virkningstidspunkt = YearMonth.of(2023, 6)
        val beregning = beregning()

        val avkorting = avkorting(avkortingGrunnlag = listOf(avkortinggrunnlag()))

        val aarsoppgjoer = listOf(aarsoppgjoer(restanse = 1000))
        val avkortingsperioder = listOf(avkortingsperiode())
        val avkortetYtelse = listOf(avkortetYtelse())

        every {
            AvkortingRegelkjoring.beregnAarsoppgjoer(avkorting, virkningstidspunkt)
        } returns aarsoppgjoer
        every {
            AvkortingRegelkjoring.beregnInntektsavkorting(
                Periode(fom = virkningstidspunkt, tom = null),
                avkorting.avkortingGrunnlag
            )
        } returns avkortingsperioder
        every {
            AvkortingRegelkjoring.beregnAvkortetYtelse(
                Periode(fom = virkningstidspunkt, tom = null),
                beregning.beregningsperioder,
                avkortingsperioder,
                maanedligRestanse = 1000
            )
        } returns avkortetYtelse

        val beregnetAvkorting = avkorting.beregnAvkorting(virkningstidspunkt, beregning)

        beregnetAvkorting.avkortingGrunnlag shouldBe avkorting.avkortingGrunnlag
        beregnetAvkorting.avkortingsperioder shouldBe avkortingsperioder
        beregnetAvkorting.aarsoppgjoer shouldBe aarsoppgjoer
        beregnetAvkorting.avkortetYtelse shouldBe avkortetYtelse
    }

    @Test
    fun `Beregn avkorting beregner nytt aarsoppgjoer for dette aaret og beholder tidligere aar uendret`() {
        val nyttAarsoppgjoer = aarsoppgjoer(maaned = YearMonth.of(2023, 1))
        val tidligereAarsoppgjoer = aarsoppgjoer(maaned = YearMonth.of(2022, 1))

        val avkorting = avkorting(
            aarsoppgjoer = listOf(tidligereAarsoppgjoer)
        )

        every { AvkortingRegelkjoring.beregnAarsoppgjoer(any(), any(), any()) } returns listOf(nyttAarsoppgjoer)
        every { AvkortingRegelkjoring.beregnInntektsavkorting(any(), any()) } returns listOf(avkortingsperiode())
        every { AvkortingRegelkjoring.beregnAvkortetYtelse(any(), any(), any(), any()) } returns listOf(
            avkortetYtelse()
        )

        val beregnetAvkorting = avkorting.beregnAvkorting(YearMonth.of(2023, 6), beregning())

        beregnetAvkorting.aarsoppgjoer shouldBe listOf(tidligereAarsoppgjoer, nyttAarsoppgjoer)
    }

    @Test
    fun `Beregn avkorting beregner nytt aarsoppgjoer og erstatter hvis det finnes fra foer`() {
        val nyttAarsoppgjoer = aarsoppgjoer(maaned = YearMonth.of(2023, 1), restanse = 2000)
        val tidligereAarsoppgjoer = aarsoppgjoer(maaned = YearMonth.of(2022, 1))

        val avkorting = avkorting(
            aarsoppgjoer = listOf(
                aarsoppgjoer(maaned = YearMonth.of(2023, 1), restanse = 1000),
                tidligereAarsoppgjoer
            )
        )

        every { AvkortingRegelkjoring.beregnAarsoppgjoer(any(), any(), any()) } returns listOf(nyttAarsoppgjoer)
        every { AvkortingRegelkjoring.beregnInntektsavkorting(any(), any()) } returns listOf(avkortingsperiode())
        every { AvkortingRegelkjoring.beregnAvkortetYtelse(any(), any(), any(), any()) } returns listOf(
            avkortetYtelse()
        )

        val beregnetAvkorting = avkorting.beregnAvkorting(YearMonth.of(2023, 6), beregning())

        beregnetAvkorting.aarsoppgjoer shouldBe listOf(tidligereAarsoppgjoer, nyttAarsoppgjoer)
    }

    @Test
    fun `Beregn avkorting beregner nytt aarsoppgjoer med tidligere avkorting hvis angitt`() {
        val virkningstidspunkt = YearMonth.of(2023, 6)
        val forrigeAvkorting = mockk<Avkorting>()
        val avkorting = avkorting()

        every { AvkortingRegelkjoring.beregnAarsoppgjoer(any(), any(), any()) } returns listOf(aarsoppgjoer())
        every { AvkortingRegelkjoring.beregnInntektsavkorting(any(), any()) } returns listOf(avkortingsperiode())
        every { AvkortingRegelkjoring.beregnAvkortetYtelse(any(), any(), any(), any()) } returns listOf(
            avkortetYtelse()
        )

        avkorting.beregnAvkorting(virkningstidspunkt, beregning(), forrigeAvkorting)

        verify { AvkortingRegelkjoring.beregnAarsoppgjoer(avkorting, virkningstidspunkt, forrigeAvkorting) }
    }
}