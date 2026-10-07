package no.nav.etterlatte.beregning.regler.avkorting

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.etterlatte.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.Etteroppgjoer
import no.nav.etterlatte.avkorting.YtelseFoerAvkorting
import no.nav.etterlatte.avkorting.finnHullIYtelse
import no.nav.etterlatte.avkorting.utenHullIYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagreDto
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.sanksjon
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

/**
 * Avkorting for OMS når beregningen har hull mellom vedtaksperioder (beregning over flere perioder).
 */
class AvkortingMedHullTest {
    @BeforeEach
    fun `mock grunnbeloep`() {
        mockkObject(GrunnbeloepRepository)
        every { GrunnbeloepRepository.historiskeGrunnbeloep } returns
            listOf(
                Grunnbeloep(
                    dato = YearMonth.of(2023, 5),
                    grunnbeloep = 118620,
                    omregningsfaktor = BigDecimal("1.045591"),
                ),
                Grunnbeloep(
                    dato = YearMonth.of(2024, 5),
                    grunnbeloep = 124028,
                    omregningsfaktor = BigDecimal("1.064076"),
                ),
            )
    }

    @AfterEach
    fun `unmock grunnbeloep`() {
        unmockkObject(GrunnbeloepRepository)
    }

    private fun beregnFoerstegangsbehandling(
        beregninger: List<Beregningsperiode>,
        inntektFom: List<YearMonth>,
        brukNyeRegler: Boolean,
        sanksjoner: List<Sanksjon> = emptyList(),
        opphoerFom: YearMonth? = null,
    ): Avkorting =
        Avkorting().beregnAvkortingMedNyeGrunnlag(
            nyttGrunnlag =
                inntektFom.map {
                    avkortinggrunnlagLagreDto(aarsinntekt = 300_000, fratrekkInnAar = 0, fom = it)
                },
            bruker = bruker,
            beregning = beregning(beregninger = beregninger),
            sanksjoner = sanksjoner,
            opphoerFom = opphoerFom,
            brukNyeReglerAvkorting = brukNyeRegler,
        )

    private fun List<AvkortetYtelse>.somOverlapper(
        fom: YearMonth,
        tom: YearMonth,
    ) = filter { it.periode.fom <= tom && (it.periode.tom ?: YearMonth.of(9999, 12)) >= fom }

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `hull innenfor samme aar teller ikke som innvilgede maaneder`(brukNyeRegler: Boolean) {
        val avkorting =
            beregnFoerstegangsbehandling(
                beregninger =
                    listOf(
                        beregningsperiode(
                            datoFOM = YearMonth.of(2024, 1),
                            datoTOM = YearMonth.of(2024, 4),
                            utbetaltBeloep = 16_000,
                        ),
                        beregningsperiode(datoFOM = YearMonth.of(2024, 9), datoTOM = null, utbetaltBeloep = 16_000),
                    ),
                inntektFom = listOf(YearMonth.of(2024, 1)),
                brukNyeRegler = brukNyeRegler,
            )

        val aarsoppgjoer = avkorting.aarsoppgjoer.single() as AarsoppgjoerLoepende
        // jan–apr (4) + sep–des (4)
        aarsoppgjoer.innvilgaMaaneder() shouldBe 8
        aarsoppgjoer.avkortetYtelse.somOverlapper(YearMonth.of(2024, 5), YearMonth.of(2024, 8)) shouldBe emptyList()
    }

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `hull over aarsskiftet gir riktige innvilgede maaneder for begge aar`(brukNyeRegler: Boolean) {
        val avkorting =
            beregnFoerstegangsbehandling(
                beregninger =
                    listOf(
                        beregningsperiode(
                            datoFOM = YearMonth.of(2024, 3),
                            datoTOM = YearMonth.of(2024, 10),
                            utbetaltBeloep = 16_000,
                        ),
                        beregningsperiode(datoFOM = YearMonth.of(2025, 3), datoTOM = null, utbetaltBeloep = 16_000),
                    ),
                inntektFom = listOf(YearMonth.of(2024, 3), YearMonth.of(2025, 3)),
                brukNyeRegler = brukNyeRegler,
            )

        val (aar2024, aar2025) = avkorting.aarsoppgjoer
        aar2024.aar shouldBe 2024
        aar2024.innvilgaMaaneder() shouldBe 8 // mar–okt
        aar2025.aar shouldBe 2025
        aar2025.innvilgaMaaneder() shouldBe 10 // mar–des

        aar2024.avkortetYtelse.somOverlapper(YearMonth.of(2024, 11), YearMonth.of(2024, 12)) shouldBe emptyList()
        aar2025.avkortetYtelse.somOverlapper(YearMonth.of(2025, 1), YearMonth.of(2025, 2)) shouldBe emptyList()
    }

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `ny inntekt etter hull i samme aar gir restanse uten ytelse i hullet`(brukNyeRegler: Boolean) {
        val avkorting =
            Avkorting().beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(aarsinntekt = 300_000, fratrekkInnAar = 0, fom = YearMonth.of(2024, 1)),
                        avkortinggrunnlagLagreDto(aarsinntekt = 400_000, fratrekkInnAar = 0, fom = YearMonth.of(2024, 9)),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, 1),
                                    datoTOM = YearMonth.of(2024, 4),
                                    utbetaltBeloep = 16_000,
                                ),
                                beregningsperiode(datoFOM = YearMonth.of(2024, 9), datoTOM = null, utbetaltBeloep = 16_000),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
                brukNyeReglerAvkorting = brukNyeRegler,
            )

        val aarsoppgjoer = avkorting.aarsoppgjoer.single()
        aarsoppgjoer.avkortetYtelse.somOverlapper(YearMonth.of(2024, 5), YearMonth.of(2024, 8)) shouldBe emptyList()
        aarsoppgjoer.avkortetYtelse
            .last()
            .periode.tom shouldBe null
    }

    @ParameterizedTest(name = "nye regler: {0}, ny inntekt fra maaned: {1}")
    @CsvSource("true, 2", "false, 2", "true, 3", "false, 3", "true, 7", "false, 7")
    fun `revurdering etter opphoer med ny inntekt beholder historikk uten overlapp eller ytelse i hullet`(
        brukNyeRegler: Boolean,
        maaned: Int,
    ) {
        val januar = YearMonth.of(2024, 1)
        val mars = YearMonth.of(2024, 3)
        val juli = YearMonth.of(2024, 7)
        val nyInntektFom = YearMonth.of(2024, maaned)
        val foerstegangsbehandling =
            beregnFoerstegangsbehandling(
                beregninger =
                    listOf(
                        beregningsperiode(datoFOM = januar, datoTOM = mars, utbetaltBeloep = 16_000),
                    ),
                inntektFom = listOf(januar),
                brukNyeRegler = brukNyeRegler,
                opphoerFom = YearMonth.of(2024, 4),
            )
        val nyeBeregninger =
            listOfNotNull(
                if (nyInntektFom <= mars) {
                    beregningsperiode(datoFOM = nyInntektFom, datoTOM = mars, utbetaltBeloep = 16_000)
                } else {
                    null
                },
                beregningsperiode(datoFOM = juli, utbetaltBeloep = 16_000),
            )

        val revurdering =
            foerstegangsbehandling.kopierAvkorting().beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(aarsinntekt = 100_000, fratrekkInnAar = 0, fom = nyInntektFom),
                    ),
                bruker = bruker,
                beregning = beregning(beregninger = nyeBeregninger),
                sanksjoner = emptyList(),
                opphoerFom = null,
                brukNyeReglerAvkorting = brukNyeRegler,
            )

        val aarsoppgjoer = revurdering.aarsoppgjoer.single().shouldBeInstanceOf<AarsoppgjoerLoepende>()
        val sisteHistoriskeMaaned = minOf(mars, nyInntektFom.minusMonths(1))
        aarsoppgjoer.inntektsavkorting.map { it.grunnlag.periode } shouldBe
            listOf(
                Periode(januar, sisteHistoriskeMaaned),
                Periode(nyInntektFom, null),
            )
        aarsoppgjoer.inntektsavkorting
            .last()
            .grunnlag.innvilgaMaaneder shouldBe 9

        val foer = foerstegangsbehandling.aarsoppgjoer.single().avkortetYtelse
        val etter = aarsoppgjoer.avkortetYtelse
        (1..sisteHistoriskeMaaned.monthValue).forEach {
            etter.ytelseI(YearMonth.of(2024, it)) shouldBe foer.ytelseI(YearMonth.of(2024, it))
        }
        etter.somOverlapper(YearMonth.of(2024, 4), YearMonth.of(2024, 6)) shouldBe emptyList()
        (listOf(1, 2, 3) + (7..12)).forEach { maanedMedYtelse ->
            etter.count {
                val dato = YearMonth.of(2024, maanedMedYtelse)
                it.periode.fom <= dato && (it.periode.tom ?: dato) >= dato
            } shouldBe 1
        }
        etter.zipWithNext().all { (forrige, neste) ->
            forrige.periode.fom < neste.periode.fom &&
                requireNotNull(forrige.periode.tom) < neste.periode.fom
        } shouldBe true
        etter.last().periode.tom shouldBe null

        foerstegangsbehandling.aarsoppgjoer
            .single()
            .shouldBeInstanceOf<AarsoppgjoerLoepende>()
            .inntektsavkorting
            .single()
            .grunnlag.periode shouldBe Periode(januar, mars)
    }

    @Test
    fun `utenHullIYtelse deler kun opp perioden rundt hull mellom perioder`() {
        val ytelse =
            listOf(
                ytelse(YearMonth.of(2024, 1), YearMonth.of(2024, 2)),
                ytelse(YearMonth.of(2024, 3), YearMonth.of(2024, 4)),
                ytelse(YearMonth.of(2024, 9), null),
            )

        finnHullIYtelse(ytelse) shouldBe listOf(Periode(YearMonth.of(2024, 5), YearMonth.of(2024, 8)))
        Periode(YearMonth.of(2024, 1), null).utenHullIYtelse(ytelse) shouldBe
            listOf(
                Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 4)),
                Periode(YearMonth.of(2024, 9), null),
            )
        Periode(YearMonth.of(2024, 6), YearMonth.of(2024, 12)).utenHullIYtelse(ytelse) shouldBe
            listOf(Periode(YearMonth.of(2024, 9), YearMonth.of(2024, 12)))
        // Lukket siste ytelse: måneder etter siste ytelse ekskluderes
        Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)).utenHullIYtelse(ytelse.take(2)) shouldBe
            listOf(Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 4)))
        // Sammenhengende og åpen ytelse: perioden er uendret
        Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)).utenHullIYtelse(
            listOf(ytelse(YearMonth.of(2024, 1), YearMonth.of(2024, 4)), ytelse(YearMonth.of(2024, 5), null)),
        ) shouldBe listOf(Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)))
    }

    private fun ytelse(
        fom: YearMonth,
        tom: YearMonth?,
    ) = YtelseFoerAvkorting(beregning = 16_000, periode = Periode(fom, tom), beregningsreferanse = UUID.randomUUID())

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `etteroppgjoer med hull i aaret gir ingen ytelse i hullet`(brukNyeRegler: Boolean) {
        val loepende =
            beregnFoerstegangsbehandling(
                beregninger = beregningerMedHull(),
                inntektFom = listOf(YearMonth.of(2024, 1)),
                brukNyeRegler = brukNyeRegler,
            )

        val avkorting =
            loepende.beregnEtteroppgjoer(
                brukerTokenInfo = bruker,
                aar = 2024,
                loennsinntekt = 350_000,
                afp = 0,
                naeringsinntekt = 0,
                utland = 0,
                sanksjoner = emptyList(),
                spesifikasjon = "",
                innvilgetPeriodeIEtteroppgjoersAar = loepende.aarsoppgjoer.single().periode(),
                opphoerFom = null,
                brukNyeReglerAvkorting = brukNyeRegler,
            )

        val etteroppgjoer = avkorting.aarsoppgjoer.single()
        etteroppgjoer.shouldBeInstanceOf<Etteroppgjoer>()
        // jan–jun (6) + okt–des (3)
        etteroppgjoer.innvilgaMaaneder() shouldBe 9
        etteroppgjoer.avkortetYtelse.somOverlapper(YearMonth.of(2024, 7), YearMonth.of(2024, 9)) shouldBe emptyList()
        etteroppgjoer.avkortetYtelse.somOverlapper(YearMonth.of(2024, 1), YearMonth.of(2024, 6)) shouldNotBe emptyList<AvkortetYtelse>()
        etteroppgjoer.avkortetYtelse.somOverlapper(YearMonth.of(2024, 10), YearMonth.of(2024, 12)) shouldNotBe emptyList<AvkortetYtelse>()
    }

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `regulering fra mai med hull etter virk gir ingen ytelse i hullet og ny ytelse etter hullet`(brukNyeRegler: Boolean) {
        val foerRegulering =
            beregnFoerstegangsbehandling(
                beregninger = beregningerMedHull(),
                inntektFom = listOf(YearMonth.of(2024, 1)),
                brukNyeRegler = brukNyeRegler,
            )
        val reguleringVirk = YearMonth.of(2024, 5)

        val etterRegulering =
            foerRegulering.kopierAvkorting().beregnAvkorting(
                virkningstidspunkt = reguleringVirk,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(datoFOM = reguleringVirk, datoTOM = YearMonth.of(2024, 6), utbetaltBeloep = 17_000),
                                beregningsperiode(datoFOM = YearMonth.of(2024, 10), datoTOM = null, utbetaltBeloep = 17_000),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
                brukNyeReglerAvkorting = brukNyeRegler,
            )

        val foer = foerRegulering.aarsoppgjoer.single().avkortetYtelse
        val etter = etterRegulering.aarsoppgjoer.single().avkortetYtelse
        etterRegulering.aarsoppgjoer.single().innvilgaMaaneder() shouldBe 9
        etter.somOverlapper(YearMonth.of(2024, 7), YearMonth.of(2024, 9)) shouldBe emptyList()
        etter.last().periode.tom shouldBe null

        // Før virk er uendret, etter virk (på begge sider av hullet) er ytelsen høyere
        etter.ytelseI(YearMonth.of(2024, 3)) shouldBe foer.ytelseI(YearMonth.of(2024, 3))
        etter.ytelseI(YearMonth.of(2024, 5)) shouldBeGreaterThan foer.ytelseI(YearMonth.of(2024, 5))
        etter.ytelseI(YearMonth.of(2024, 11)) shouldBeGreaterThan foer.ytelseI(YearMonth.of(2024, 11))
    }

    private fun beregningerMedHull() =
        listOf(
            beregningsperiode(datoFOM = YearMonth.of(2024, 1), datoTOM = YearMonth.of(2024, 6), utbetaltBeloep = 16_000),
            beregningsperiode(datoFOM = YearMonth.of(2024, 10), datoTOM = null, utbetaltBeloep = 16_000),
        )

    private fun List<AvkortetYtelse>.ytelseI(maaned: YearMonth): Int =
        single { it.periode.fom <= maaned && (it.periode.tom ?: maaned) >= maaned }.ytelseEtterAvkorting

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `sanksjon som krysser hullet gir 0 i sanksjonsmaaneder og ingen ytelse i hullet`(brukNyeRegler: Boolean) {
        val avkorting =
            beregnFoerstegangsbehandling(
                beregninger = beregningerMedHull(),
                inntektFom = listOf(YearMonth.of(2024, 1)),
                brukNyeRegler = brukNyeRegler,
                sanksjoner = listOf(sanksjon(fom = YearMonth.of(2024, 5), tom = YearMonth.of(2024, 10))),
            )

        val ytelse = avkorting.aarsoppgjoer.single().avkortetYtelse
        ytelse.somOverlapper(YearMonth.of(2024, 7), YearMonth.of(2024, 9)) shouldBe emptyList()
        ytelse.ytelseI(YearMonth.of(2024, 4)) shouldBeGreaterThan 0
        ytelse.ytelseI(YearMonth.of(2024, 5)) shouldBe 0
        ytelse.ytelseI(YearMonth.of(2024, 6)) shouldBe 0
        ytelse.ytelseI(YearMonth.of(2024, 10)) shouldBe 0
        ytelse.ytelseI(YearMonth.of(2024, 11)) shouldBeGreaterThan 0
        ytelse.last().periode.tom shouldBe null
    }

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `opphoer i hullet gir siste ytelse til og med slutten av perioden foer hullet`(brukNyeRegler: Boolean) {
        // Beregningen har allerede kuttet perioden etter hullet pga. opphør (tilDato)
        val avkorting =
            beregnFoerstegangsbehandling(
                beregninger =
                    listOf(
                        beregningsperiode(datoFOM = YearMonth.of(2024, 1), datoTOM = YearMonth.of(2024, 6), utbetaltBeloep = 16_000),
                    ),
                inntektFom = listOf(YearMonth.of(2024, 1)),
                brukNyeRegler = brukNyeRegler,
                opphoerFom = YearMonth.of(2024, 8),
            )

        val ytelse = avkorting.aarsoppgjoer.single().avkortetYtelse
        ytelse.last().periode.tom shouldBe YearMonth.of(2024, 6)
        ytelse.somOverlapper(YearMonth.of(2024, 7), YearMonth.of(2024, 12)) shouldBe emptyList()
    }

    @ParameterizedTest(name = "nye regler: {0}")
    @ValueSource(booleans = [true, false])
    fun `opphoer etter hullet gir ytelse paa begge sider av hullet og lukket siste periode`(brukNyeRegler: Boolean) {
        val avkorting =
            beregnFoerstegangsbehandling(
                beregninger =
                    listOf(
                        beregningsperiode(datoFOM = YearMonth.of(2024, 1), datoTOM = YearMonth.of(2024, 6), utbetaltBeloep = 16_000),
                        beregningsperiode(datoFOM = YearMonth.of(2024, 10), datoTOM = YearMonth.of(2024, 10), utbetaltBeloep = 16_000),
                    ),
                inntektFom = listOf(YearMonth.of(2024, 1)),
                brukNyeRegler = brukNyeRegler,
                opphoerFom = YearMonth.of(2024, 11),
            )

        val aarsoppgjoer = avkorting.aarsoppgjoer.single()
        aarsoppgjoer.innvilgaMaaneder() shouldBe 7
        val ytelse = aarsoppgjoer.avkortetYtelse
        ytelse.somOverlapper(YearMonth.of(2024, 7), YearMonth.of(2024, 9)) shouldBe emptyList()
        // Høy månedlig inntekt over få innvilgede måneder kan gi 0 kr, så vi sjekker kun at perioden finnes
        ytelse.somOverlapper(YearMonth.of(2024, 10), YearMonth.of(2024, 10)) shouldNotBe emptyList<AvkortetYtelse>()
        ytelse.last().periode.tom shouldBe YearMonth.of(2024, 10)
    }
}
