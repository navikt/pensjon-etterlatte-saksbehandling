package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.OffsetDateTime
import java.time.YearMonth

class InntektskomponentBeregningTest {
    @Test
    fun `kan beregne inntekter gitt fra A-inntektskomponent med tomt resultat`() {
        val inntektsgrunnlag =
            InntektBulkResponsDto(
                filter = "test",
                data = listOf(),
            )

        val beregnet =
            InntektskomponentBeregning.beregnInntekt(
                inntektsgrunnlag,
                2024,
            )

        beregnet.verdi.inntekter shouldHaveSize 12
        beregnet.verdi.inntekter.forEach { it.beloep shouldBe BigDecimal.ZERO }
        beregnet.verdi.inntekter.forEach { it.maaned.year shouldBe 2024 }
        beregnet.verdi.inntekter
            .map { it.maaned }
            .toSet() shouldHaveSize 12
    }

    @Test
    fun `gir feilmelding med inntekter utenfor året`() {
        val inntektsgrunnlag =
            InntektBulkResponsDto(
                filter = "test",
                data =
                    listOf(
                        inntektsinformasjonDto(
                            maaned = YearMonth.of(2025, Month.JANUARY),
                            inntektListe =
                                listOf(
                                    inntektDto(BigDecimal.ONE),
                                ),
                        ),
                    ),
            )

        shouldThrow<InternfeilException> {
            InntektskomponentBeregning.beregnInntekt(
                inntektsgrunnlag,
                2024,
            )
        }
    }

    @Test
    fun `kan summere flere instanser av samme periode`() {
        val inntektsgrunnlag =
            InntektBulkResponsDto(
                filter = "test",
                data =
                    listOf(
                        inntektsinformasjonDto(
                            maaned = YearMonth.of(2024, Month.JANUARY),
                            inntektListe =
                                listOf(
                                    inntektDto(BigDecimal.ONE),
                                ),
                        ),
                        inntektsinformasjonDto(
                            maaned = YearMonth.of(2024, Month.JANUARY),
                            inntektListe =
                                listOf(
                                    inntektDto(BigDecimal.ONE),
                                ),
                        ),
                        inntektsinformasjonDto(
                            maaned = YearMonth.of(2024, Month.FEBRUARY),
                            inntektListe =
                                listOf(
                                    inntektDto(BigDecimal.ONE),
                                ),
                        ),
                    ),
            )
        val beregnet =
            InntektskomponentBeregning.beregnInntekt(
                inntektsgrunnlag,
                2024,
            )
        beregnet.verdi.inntekter shouldHaveSize 12
        beregnet.verdi.inntekter shouldHaveSize 12
        beregnet.verdi.inntekter.forEach {
            when (it.maaned) {
                YearMonth.of(2024, Month.JANUARY) -> it.beloep shouldBe BigDecimal.TWO
                YearMonth.of(2024, Month.FEBRUARY) -> it.beloep shouldBe BigDecimal.ONE
                else -> it.beloep shouldBe BigDecimal.ZERO
            }
        }
        beregnet.verdi.inntekter.forEach { it.maaned.year shouldBe 2024 }
        beregnet.verdi.inntekter
            .map { it.maaned }
            .toSet() shouldHaveSize 12
    }
}

private fun inntektsinformasjonDto(
    inntektListe: List<InntektDto>,
    maaned: YearMonth,
): InntektsinformasjonDto =
    InntektsinformasjonDto(
        maaned = maaned,
        opplysningspliktig = "",
        underenhet = "",
        norskident = "",
        oppsummeringstidspunkt = OffsetDateTime.now(),
        inntektListe = inntektListe,
    )

private fun inntektDto(beloep: BigDecimal): InntektDto =
    InntektDto(
        type = "",
        beloep = beloep,
        fordel = "",
        beskrivelse = "",
        inngaarIGrunnlagForTrekk = false,
        utloeserArbeidsgiveravgift = false,
        opptjeningsperiodeFom = LocalDate.now(),
        opptjeningsperiodeTom = LocalDate.now(),
    )
