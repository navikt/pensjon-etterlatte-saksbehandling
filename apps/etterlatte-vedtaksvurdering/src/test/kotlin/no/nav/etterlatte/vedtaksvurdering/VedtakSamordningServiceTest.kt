package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class VedtakSamordningServiceTest {
    private val vedtakRepository: VedtaksvurderingRepository = mockk()
    private val samordningService = VedtakSamordningService(vedtakRepository)

    @Test
    fun `skal hente vedtak`() {
        every { vedtakRepository.hentVedtak(1234) } returns
            vedtak(
                status = VedtakStatus.IVERKSATT,
                avkorting =
                    objectMapper.valueToTree(
                        AvkortingDto(
                            avkortingGrunnlag = emptyList(),
                            avkortetYtelse = emptyList(),
                            tidligereAvkortetYtelse = emptyList(),
                        ),
                    ),
            )

        val resultat = samordningService.hentVedtak(1234)!!

        verify { vedtakRepository.hentVedtak(1234) }

        resultat.status shouldBeEqual VedtakStatus.IVERKSATT
        resultat.perioder shouldBeEqual emptyList()
    }

    @Test
    fun `Skal hente sammenstilt tidslinje av vedtakslista`() {
        val virkFom2024Januar = YearMonth.of(2024, Month.JANUARY)
        val virkFom2024Februar = YearMonth.of(2024, Month.FEBRUARY)
        val ident = Folkeregisteridentifikator.of(FNR_2)

        every { vedtakRepository.hentFerdigstilteVedtak(ident, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                vedtak(
                    status = VedtakStatus.IVERKSATT,
                    virkningstidspunkt = virkFom2024Januar,
                    vedtakFattet =
                        VedtakFattet(
                            "SBH",
                            "1014",
                            Tidspunkt.parse("2023-12-05T14:20:50Z"),
                        ),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                periode = Periode(virkFom2024Januar, null),
                                beloep = BigDecimal(2500),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                        ),
                    avkorting =
                        objectMapper.valueToTree(
                            avkortingDto(fom = virkFom2024Januar, ytelseFoer = 3000, ytelseEtter = 2500),
                        ),
                ),
                vedtak(
                    status = VedtakStatus.IVERKSATT,
                    virkningstidspunkt = virkFom2024Februar,
                    vedtakFattet =
                        VedtakFattet(
                            "SBH",
                            "1014",
                            Tidspunkt.parse("2024-01-11T09:43:04Z"),
                        ),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                periode = Periode(virkFom2024Februar, null),
                                beloep = BigDecimal(2500),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                        ),
                    avkorting =
                        objectMapper.valueToTree(
                            avkortingDto(fom = virkFom2024Februar, ytelseFoer = 3200, ytelseEtter = 2900),
                        ),
                ),
            )

        val resultat =
            samordningService.hentVedtaksliste(
                fnr = ident,
                sakType = SakType.OMSTILLINGSSTOENAD,
                fomDato = LocalDate.of(2024, Month.JANUARY, 1),
            )

        verify { vedtakRepository.hentFerdigstilteVedtak(ident, SakType.OMSTILLINGSSTOENAD) }

        assertAll(
            { resultat shouldHaveSize 2 },
            { resultat[0].virkningstidspunkt shouldBe virkFom2024Januar },
            { resultat[0].perioder shouldHaveSize 1 },
            { resultat[0].perioder[0].fom shouldBe virkFom2024Januar },
            { resultat[0].perioder[0].tom shouldBe virkFom2024Januar },
            { resultat[1].virkningstidspunkt shouldBe virkFom2024Februar },
            { resultat[1].perioder shouldHaveSize 1 },
            { resultat[1].perioder[0].fom shouldBe virkFom2024Februar },
            { resultat[1].perioder[0].tom shouldBe null },
        )
    }

    private fun avkortingDto(
        fom: YearMonth,
        ytelseFoer: Int,
        ytelseEtter: Int,
    ) = AvkortingDto(
        avkortingGrunnlag = emptyList(),
        avkortetYtelse =
            listOf(
                AvkortetYtelseDto(
                    fom = fom,
                    tom = null,
                    ytelseFoerAvkorting = ytelseFoer,
                    ytelseEtterAvkorting = ytelseEtter,
                    avkortingsbeloep = ytelseEtter - ytelseFoer,
                    restanse = 0,
                    sanksjon = null,
                ),
            ),
        tidligereAvkortetYtelse = emptyList(),
    )
}
