package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import kotlin.random.Random

internal class VedtakEtterbetalingTest {
    private val januar2024 = YearMonth.of(2024, Month.JANUARY)
    private val februar2024 = YearMonth.of(2024, Month.FEBRUARY)
    private val mai2024 = YearMonth.of(2024, Month.MAY)
    private val mars2024 = YearMonth.of(2024, Month.MARCH)

    private val klokkeJanuar2024: Clock = Clock.fixed(Instant.parse("2024-01-20T11:00:00Z"), norskTidssone)
    private val klokkeMars2024: Clock = Clock.fixed(Instant.parse("2024-03-06T10:00:00Z"), norskTidssone)
    private val grunnbeloep =
        mockk<Grunnbeloep> {
            every { grunnbeloep } returns 124028
        }

    private val repository = mockk<VedtaksvurderingRepository>()

    @AfterEach
    fun afterEach() = clearAllMocks()

    @Test
    fun `ikke etterbetaling grunnet ikke tilbake i tid`() {
        val vedtak = vedtak(sakType = SakType.OMSTILLINGSSTOENAD, virkningstidspunkt = februar2024)

        every { repository.hentFerdigstilteVedtak(vedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns emptyList()

        val resultat = vedtak.erVedtakMedEtterbetaling(repository, grunnbeloep, klokkeJanuar2024)

        verify { repository wasNot called }

        resultat shouldBe EtterbetalingResultat.ingen()
    }

    @Test
    fun `etterbetaling grunnet utbetaling tilbake i tid og ingen tidligere periode`() {
        val vedtak = vedtak(sakType = SakType.OMSTILLINGSSTOENAD, virkningstidspunkt = februar2024)

        every { repository.hentFerdigstilteVedtak(vedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns emptyList()

        val resultat = vedtak.erVedtakMedEtterbetaling(repository, grunnbeloep, klokkeMars2024)

        verify { repository.hentFerdigstilteVedtak(vedtak.soeker, SakType.OMSTILLINGSSTOENAD) }

        resultat shouldBe EtterbetalingResultat(erEtterbetaling = true, harUtvidetFrist = false)
    }

    @Test
    fun `ikke etterbetaling, tidligere periode men samme beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                id = 2L,
                virkningstidspunkt = februar2024,
                beloep = 2500,
                fattetTidspunkt = "2024-01-29T14:05:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    id = 1L,
                    virkningstidspunkt = januar2024,
                    beloep = 2500,
                    fattetTidspunkt = "2024-01-26T11:25:00Z",
                ),
            )

        val resultat = nyttVedtak.erVedtakMedEtterbetaling(repository, grunnbeloep, klokkeMars2024)

        resultat shouldBe EtterbetalingResultat.ingen()
    }

    @Test
    fun `ikke etterbetaling, siste periode er opphoer uten beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                id = 2L,
                virkningstidspunkt = februar2024,
                beloep = null,
                fattetTidspunkt = "2024-10-29T14:05:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    id = 1L,
                    virkningstidspunkt = februar2024,
                    beloep = 3000,
                    fattetTidspunkt = "2024-01-26T11:25:00Z",
                ),
                aVedtakMedUtbetalingsperiode(
                    id = 1L,
                    virkningstidspunkt = mai2024,
                    beloep = 4000,
                    fattetTidspunkt = "2024-01-26T11:25:00Z",
                ),
            )

        val resultat =
            nyttVedtak.erVedtakMedEtterbetaling(
                repository,
                grunnbeloep,
                Clock.fixed(Instant.parse("2024-10-23T10:00:00Z"), norskTidssone),
            )

        resultat shouldBe EtterbetalingResultat.ingen()
    }

    @Test
    fun `etterbetaling - ny periode har hoeyere beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                id = 2L,
                virkningstidspunkt = februar2024,
                beloep = 3500,
                fattetTidspunkt = "2024-01-26T11:25:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    id = 1L,
                    virkningstidspunkt = januar2024,
                    beloep = 2500,
                    fattetTidspunkt = "2023-12-16T13:30:00Z",
                ),
            )

        val resultat = nyttVedtak.erVedtakMedEtterbetaling(repository, grunnbeloep, klokkeMars2024)

        resultat shouldBe EtterbetalingResultat(erEtterbetaling = true)
    }

    @Test
    fun `etterbetaling - flere tidligere perioder, ny periode har hoeyere beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                id = 10L,
                virkningstidspunkt = februar2024,
                beloep = 3500,
                fattetTidspunkt = "2024-03-06T13:30:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    id = 1L,
                    virkningstidspunkt = januar2024,
                    beloep = 2000,
                    fattetTidspunkt = "2024-01-07T13:30:00Z",
                ),
                aVedtakMedUtbetalingsperiode(
                    id = 2L,
                    virkningstidspunkt = februar2024,
                    beloep = 2500,
                    fattetTidspunkt = "2024-01-26T14:00:00Z",
                ),
                aVedtakMedUtbetalingsperiode(
                    id = 3L,
                    virkningstidspunkt = mars2024,
                    beloep = 3000,
                    fattetTidspunkt = "2024-03-05T10:00:00Z",
                ),
            )

        val resultat = nyttVedtak.erVedtakMedEtterbetaling(repository, grunnbeloep, klokkeMars2024)

        resultat shouldBe EtterbetalingResultat(erEtterbetaling = true)
    }

    @Test
    fun `Skal ha utvidet frist hvis etterbetaling er mer enn en halv G`() {
        val virkningstidspunkt = YearMonth.of(2024, Month.JULY)

        val nyttVedtak =
            vedtak(
                id = Random.nextLong(),
                sakType = SakType.OMSTILLINGSSTOENAD,
                virkningstidspunkt = virkningstidspunkt,
                vedtakFattet =
                    VedtakFattet(
                        "Z01",
                        ENHET_1,
                        Tidspunkt.parse("2024-03-06T13:30:00Z"),
                    ),
                utbetalingsperioder =
                    listOf(
                        mockPeriode(
                            beloep = 14600,
                            fom = virkningstidspunkt,
                            tom = YearMonth.of(2024, Month.DECEMBER),
                        ),
                        mockPeriode(
                            beloep = 6460,
                            fom = YearMonth.of(2024, Month.NOVEMBER),
                        ),
                    ),
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    id = 1L,
                    virkningstidspunkt = virkningstidspunkt,
                    beloep = 9302,
                    fattetTidspunkt = "2024-01-07T14:00:00Z",
                ),
                aVedtakMedUtbetalingsperiode(
                    id = 2L,
                    virkningstidspunkt = virkningstidspunkt.plusMonths(1),
                    beloep = 3876,
                    fattetTidspunkt = "2024-02-07T13:30:00Z",
                ),
            )

        val resultat =
            nyttVedtak.erVedtakMedEtterbetaling(
                repository,
                grunnbeloep,
                Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), norskTidssone),
            )

        // Skal gi en etterbetaling på kr. 64.086,- som er over 0.5G (kr. 62.014,-)
        resultat shouldBe EtterbetalingResultat(erEtterbetaling = true, harUtvidetFrist = true)
    }

    private fun aVedtakMedUtbetalingsperiode(
        id: Long,
        virkningstidspunkt: YearMonth,
        beloep: Long?,
        fattetTidspunkt: String,
    ) = vedtak(
        id = id,
        sakType = SakType.OMSTILLINGSSTOENAD,
        virkningstidspunkt = virkningstidspunkt,
        status = VedtakStatus.IVERKSATT,
        vedtakFattet =
            VedtakFattet(
                "Z01",
                ENHET_1,
                Tidspunkt.parse(fattetTidspunkt),
            ),
        utbetalingsperioder =
            listOf(
                Utbetalingsperiode(
                    id = Random.nextLong(),
                    periode = Periode(virkningstidspunkt, null),
                    beloep = beloep?.let { BigDecimal.valueOf(it) },
                    type = UtbetalingsperiodeType.UTBETALING,
                    regelverk = Regelverk.fraDato(virkningstidspunkt.atDay(1)),
                ),
            ),
    )

    private fun mockPeriode(
        beloep: Long?,
        fom: YearMonth,
        tom: YearMonth? = null,
    ) = Utbetalingsperiode(
        id = Random.nextLong(),
        periode = Periode(fom, tom),
        beloep = beloep?.let { BigDecimal.valueOf(it) },
        type = UtbetalingsperiodeType.UTBETALING,
        regelverk = Regelverk.fraDato(januar2024.atDay(1)),
    )

    @Nested
    internal inner class PeriodeOverlapp {
        @Test
        fun `ingen overlapp - lukkede perioder`() {
            val periodeA = Periode(YearMonth.of(2024, Month.JANUARY), YearMonth.of(2024, Month.FEBRUARY))
            val periodeB = Periode(YearMonth.of(2024, Month.MARCH), YearMonth.of(2024, Month.APRIL))

            periodeA.overlapper(periodeB) shouldBe false
            periodeB.overlapper(periodeA) shouldBe false
        }

        @Test
        fun `ingen overlapp - siste periode er aapen`() {
            val periodeA = Periode(YearMonth.of(2024, Month.JANUARY), YearMonth.of(2024, Month.FEBRUARY))
            val periodeB = Periode(YearMonth.of(2024, Month.MARCH), null)

            periodeA.overlapper(periodeB) shouldBe false
            periodeB.overlapper(periodeA) shouldBe false
        }

        @Test
        fun `overlapp - foerste periode er aapen`() {
            val periodeB = Periode(YearMonth.of(2024, Month.JANUARY), null)
            val periodeA = Periode(YearMonth.of(2024, Month.FEBRUARY), YearMonth.of(2024, Month.APRIL))

            periodeA.overlapper(periodeB) shouldBe true
            periodeB.overlapper(periodeA) shouldBe true
        }

        @Test
        fun `overlapp - start og slutt samme maaned, foerste periode er aapen`() {
            val periodeB = Periode(YearMonth.of(2024, Month.JANUARY), null)
            val periodeA = Periode(YearMonth.of(2024, Month.JANUARY), YearMonth.of(2024, Month.JANUARY))

            periodeA.overlapper(periodeB) shouldBe true
            periodeB.overlapper(periodeA) shouldBe true
        }

        @Test
        fun `overlapp - begge perioder er aapne`() {
            val periodeB = Periode(YearMonth.of(2024, Month.JANUARY), null)
            val periodeA = Periode(YearMonth.of(2024, Month.FEBRUARY), null)

            periodeA.overlapper(periodeB) shouldBe true
            periodeB.overlapper(periodeA) shouldBe true
        }
    }
}
