package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import vedtaksvurdering.vedtak
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import kotlin.random.Random

class VedtakEtterbetalingKtTest {
    private val januar2024 = YearMonth.of(2024, Month.JANUARY)
    private val februar2024 = YearMonth.of(2024, Month.FEBRUARY)
    private val mars2024 = YearMonth.of(2024, Month.MARCH)

    private val klokkeMars2023: Clock = Clock.fixed(Instant.parse("2024-03-06T10:00:00Z"), norskTidssone)

    private val repository = mockk<VedtaksvurderingRepository>()

    @Test
    fun `ikke etterbetaling, ingen tidligere periode`() {
        val vedtak = vedtak(virkningstidspunkt = februar2024)

        every { repository.hentFerdigstilteVedtak(vedtak.soeker) } returns emptyList()

        val resultat = erVedtakMedEtterbetaling(vedtak, repository, klokkeMars2023)

        verify { repository.hentFerdigstilteVedtak(vedtak.soeker) }

        resultat shouldBe false
    }

    @Test
    fun `ikke etterbetaling, tidligere periode men samme beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                virkningstidspunkt = februar2024,
                beloep = 2500,
                fattetTidspunkt = "2024-01-29T14:05:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    virkningstidspunkt = januar2024,
                    beloep = 2500,
                    fattetTidspunkt = "2024-01-26T11:25:00Z",
                ),
            )

        val resultat = erVedtakMedEtterbetaling(nyttVedtak, repository, klokkeMars2023)

        resultat shouldBe false
    }

    @Test
    fun `etterbetaling - ny periode har hoeyere beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                virkningstidspunkt = februar2024,
                beloep = 3500,
                fattetTidspunkt = "2024-01-26T11:25:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    virkningstidspunkt = januar2024,
                    beloep = 2500,
                    fattetTidspunkt = "2023-12-16T13:30:00Z",
                ),
            )

        val resultat = erVedtakMedEtterbetaling(nyttVedtak, repository, klokkeMars2023)

        resultat shouldBe true
    }

    @Test
    fun `etterbetaling - flere tidligere perioder, ny periode har hoeyere beloep`() {
        val nyttVedtak =
            aVedtakMedUtbetalingsperiode(
                virkningstidspunkt = februar2024,
                beloep = 3500,
                fattetTidspunkt = "2024-03-06T13:30:00Z",
            )

        every { repository.hentFerdigstilteVedtak(nyttVedtak.soeker) } returns
            listOf(
                aVedtakMedUtbetalingsperiode(
                    virkningstidspunkt = januar2024,
                    beloep = 2000,
                    fattetTidspunkt = "2024-01-07T13:30:00Z",
                ),
                aVedtakMedUtbetalingsperiode(
                    virkningstidspunkt = februar2024,
                    beloep = 2500,
                    fattetTidspunkt = "2024-01-26T14:00:00Z",
                ),
                aVedtakMedUtbetalingsperiode(
                    virkningstidspunkt = mars2024,
                    beloep = 3000,
                    fattetTidspunkt = "2024-03-05T10:00:00Z",
                ),
            )

        val resultat = erVedtakMedEtterbetaling(nyttVedtak, repository, klokkeMars2023)

        resultat shouldBe true
    }

    private fun aVedtakMedUtbetalingsperiode(
        virkningstidspunkt: YearMonth,
        beloep: Long,
        fattetTidspunkt: String,
    ) = vedtak(
        virkningstidspunkt = virkningstidspunkt,
        vedtakFattet =
            VedtakFattet(
                "Z01",
                "1234",
                Tidspunkt.parse(fattetTidspunkt),
            ),
        utbetalingsperioder =
            listOf(
                Utbetalingsperiode(
                    id = Random.nextLong(),
                    periode = Periode(virkningstidspunkt, null),
                    beloep = BigDecimal.valueOf(beloep),
                    type = UtbetalingsperiodeType.UTBETALING,
                ),
            ),
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
