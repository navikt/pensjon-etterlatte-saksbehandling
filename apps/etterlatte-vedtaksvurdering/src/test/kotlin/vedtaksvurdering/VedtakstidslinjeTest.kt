package vedtaksvurdering

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingInnhold
import no.nav.etterlatte.vedtaksvurdering.Vedtakstidslinje
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID

internal class VedtakstidslinjeTest {
    private val fraOgMed = LocalDate.of(2023, 5, 1)

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * Ingen vedtak
     * */
    @Test
    fun `sak uten vedtak er ikke loepende`() {
        val actual = Vedtakstidslinje(emptyList()).erLoependePaa(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * |------------------Fattet, ikke iverksatt--------------------|
     * */
    @Test
    fun `sak uten iverksatte vedtak er ikke loepende`() {
        val fattetVedtak =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 1, 1),
                vedtakStatus = VedtakStatus.FATTET_VEDTAK,
            )
        val actual = Vedtakstidslinje(listOf(fattetVedtak)).erLoependePaa(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * |-----------------------Iverksatt----------------------------|
     * */
    @Test
    fun `sak med iverksatt foerstegangsbehandling med vedtaksdato foer fraOgMed er loepende`() {
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 1, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato)).erLoependePaa(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * |-----------------------Iverksatt----------------------------|
     *                |-----------------Opphør----------------------|
     * */
    @Test
    fun `sak som blir opphoert foer fraOgMed er ikke loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 1, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val opphoertDato =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2023, 4, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )

        val actual =
            Vedtakstidslinje(
                listOf(
                    iverksattDato,
                    opphoertDato,
                ),
            ).erLoependePaa(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * |-----------------------Iverksatt----------------------------|
     *                           |-------------Opphør---------------|
     * */
    @Test
    fun `sak som blir opphoert maaneden etter fraOgMed er loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 1, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val opphoertDato =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )

        val actual =
            Vedtakstidslinje(
                listOf(
                    iverksattDato,
                    opphoertDato,
                ),
            ).erLoependePaa(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                           |---------Iverksatt----------------|
     * */
    @Test
    fun `sak som er iverksatt maanaden etter fraOgMed-datoen er loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato)).erLoependePaa(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                           |---------Iverksatt----------------|
     *                                |---------Opphør--------------|
     * */
    @Test
    fun `sak som er iverksatt etter fraOgMed og opphoert etterpaa er loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val opphoertDato =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2023, 7, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).erLoependePaa(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                           |-----------Iverksatt--------------|
     *                           |------------Opphør----------------|
     * */
    @Test
    fun `sak som er iverksatt maanaden etter fraOgMed og opphoert samme maanad er ikke loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val opphoertDato =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).erLoependePaa(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                           |-----------Iverksatt--------------|
     *                           |------------Revurdering-----------|
     * */
    @Test
    fun `sak som er iverksatt maanaden etter fraOgMed og revurdert samme maanad er loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val opphoertDato =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).erLoependePaa(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
    }

    @Nested
    inner class Sammenstill {
        private val januar2024 = YearMonth.of(2024, Month.JANUARY)
        private val feb2024 = YearMonth.of(2024, Month.FEBRUARY)
        private val mars2024 = YearMonth.of(2024, Month.MARCH)
        private val april2024 = YearMonth.of(2024, Month.APRIL)
        private val mai2024 = YearMonth.of(2024, Month.MAY)

        @Test
        fun `skal benytte vedtak iverksatt foer angitt dato`() {
            val sammenstilt =
                Vedtakstidslinje(
                    listOf(
                        lagStandardVedtakMedEnAapenUtbetalingsperiode(
                            id = 15,
                            virkningFom = januar2024,
                            vedtakFattetDato = Tidspunkt.now().minus(50, DAYS),
                        ),
                    ),
                ).sammenstill(feb2024)

            assertAll(
                { sammenstilt.size shouldBe 1 },
                { sammenstilt[0].id shouldBe 15 },
                { sammenstilt[0].utbetalingsperioder.size shouldBe 1 },
                { sammenstilt[0].utbetalingsperioder[0].id shouldBe 150 },
            )
        }

        @Test
        fun `Skal filtrere vekk tidligere vedtak dersom siste overlapper i sin helhet`() {
            val vedtakstidslinje =
                Vedtakstidslinje(
                    listOf(
                        lagStandardVedtakMedEnAapenUtbetalingsperiode(
                            id = 1,
                            virkningFom = januar2024,
                            vedtakFattetDato = Tidspunkt.now().minus(50, DAYS),
                        ),
                        lagStandardVedtakMedEnAapenUtbetalingsperiode(
                            id = 2,
                            virkningFom = feb2024,
                            vedtakFattetDato = Tidspunkt.now().minus(30, DAYS),
                        ),
                        lagStandardVedtakMedEnAapenUtbetalingsperiode(
                            id = 3,
                            virkningFom = januar2024,
                            vedtakFattetDato = Tidspunkt.now().minus(10, DAYS),
                        ),
                    ),
                )

            val sammenstilt =
                vedtakstidslinje.sammenstill(januar2024)

            assertAll(
                { sammenstilt.size shouldBe 1 },
                { sammenstilt[0].id shouldBe 3 },
                { sammenstilt[0].utbetalingsperioder.size shouldBe 1 },
                { sammenstilt[0].utbetalingsperioder[0].id shouldBe 30 },
            )
        }

        @Test
        fun `To vedtak skal gi perioder hvor periode paa vedtak 1 er lukket dagen foer vedtak 2 sin virkningsdato`() {
            val vedtakFomJanuar2024 =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 1,
                    virkningFom = januar2024,
                    vedtakFattetDato = Tidspunkt.parse("2023-12-08T11:00:00Z"),
                )

            val vedtakFomMars2024 =
                lagVedtak(
                    id = 2,
                    virkningsDato = mars2024.atDay(1),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.parse("2024-02-17T13:30:00Z"),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                id = 20,
                                periode = Periode(mars2024, null),
                                beloep = BigDecimal.valueOf(140),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                        ),
                )

            val sammenstilt =
                Vedtakstidslinje(listOf(vedtakFomJanuar2024, vedtakFomMars2024))
                    .sammenstill(januar2024)

            assertAll(
                { sammenstilt.size shouldBe 2 },
                { sammenstilt[0].utbetalingsperioder.size shouldBe 1 },
                { sammenstilt[0].utbetalingsperioder[0].periode.fom shouldBe januar2024 },
                { sammenstilt[0].utbetalingsperioder[0].periode.tom shouldBe feb2024 },
                { sammenstilt[1].utbetalingsperioder.size shouldBe 1 },
                { sammenstilt[1].utbetalingsperioder[0].periode.fom shouldBe mars2024 },
                { sammenstilt[1].utbetalingsperioder[0].periode.tom shouldBe null },
            )
        }

        @Test
        fun `Skal filtrere vekk periode 3, samt lukke periode 2 tidligere, for det tidligste vedtaket`() {
            val vedtakFomJanuar2024 =
                lagVedtak(
                    id = 1,
                    virkningsDato = januar2024.atDay(1),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.parse("2023-12-17T13:30:00Z"),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                id = 10,
                                periode = Periode(januar2024, januar2024),
                                beloep = BigDecimal.valueOf(140),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                            Utbetalingsperiode(
                                id = 11,
                                periode = Periode(feb2024, april2024),
                                beloep = BigDecimal.valueOf(150),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                            Utbetalingsperiode(
                                id = 12,
                                periode = Periode(mai2024, null),
                                beloep = BigDecimal.valueOf(160),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                        ),
                )

            val vedtakFomMars2024 =
                lagVedtak(
                    id = 2,
                    virkningsDato = mars2024.atDay(1),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.parse("2024-02-02T13:30:00Z"),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                id = 20,
                                periode = Periode(mars2024, null),
                                beloep = BigDecimal.valueOf(180),
                                type = UtbetalingsperiodeType.UTBETALING,
                            ),
                        ),
                )

            val sammenstilt =
                Vedtakstidslinje(listOf(vedtakFomJanuar2024, vedtakFomMars2024))
                    .sammenstill(januar2024)

            assertAll(
                { sammenstilt.size shouldBe 2 },
                { sammenstilt[0].utbetalingsperioder.size shouldBe 2 },
                { sammenstilt[0].utbetalingsperioder[0].periode.fom shouldBe januar2024 },
                { sammenstilt[0].utbetalingsperioder[0].periode.tom shouldBe januar2024 },
                { sammenstilt[0].utbetalingsperioder[1].periode.fom shouldBe feb2024 },
                { sammenstilt[0].utbetalingsperioder[1].periode.tom shouldBe feb2024 },
                { sammenstilt[1].utbetalingsperioder.size shouldBe 1 },
                { sammenstilt[1].utbetalingsperioder[0].periode.fom shouldBe mars2024 },
                { sammenstilt[1].utbetalingsperioder[0].periode.tom shouldBe null },
            )
        }

        private val Vedtak.utbetalingsperioder: List<Utbetalingsperiode>
            get() = (this.innhold as VedtakBehandlingInnhold).utbetalingsperioder
    }
}

private fun lagVedtak(
    id: Long,
    virkningsDato: LocalDate,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    vedtakStatus: VedtakStatus,
    datoAttestert: Tidspunkt = Tidspunkt.now(),
    vedtakType: VedtakType = VedtakType.INNVILGELSE,
    vedtakFattetDato: Tidspunkt = Tidspunkt.now(),
    utbetalingsperioder: List<Utbetalingsperiode> = emptyList(),
): Vedtak {
    return Vedtak(
        id = id,
        sakId = 1L,
        sakType = SakType.BARNEPENSJON,
        behandlingId = UUID.randomUUID(),
        soeker = SOEKER_FOEDSELSNUMMER,
        status = vedtakStatus,
        type = vedtakType,
        vedtakFattet =
            VedtakFattet(
                ansvarligSaksbehandler = SAKSBEHANDLER_1,
                ansvarligEnhet = ENHET_1,
                tidspunkt = vedtakFattetDato,
            ),
        attestasjon =
            Attestasjon(
                attestant = SAKSBEHANDLER_2,
                attesterendeEnhet = ENHET_2,
                tidspunkt = datoAttestert,
            ),
        innhold =
            VedtakBehandlingInnhold(
                virkningstidspunkt = virkningsDato.let { YearMonth.from(it) },
                behandlingType = behandlingType,
                beregning = null,
                avkorting = null,
                vilkaarsvurdering = null,
                utbetalingsperioder = utbetalingsperioder,
                revurderingAarsak = null,
            ),
    )
}

private fun lagStandardVedtakMedEnAapenUtbetalingsperiode(
    id: Long,
    virkningFom: YearMonth,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    vedtakFattetDato: Tidspunkt = Tidspunkt.ofNorskTidssone(virkningFom.atDay(1), tid = LocalTime.NOON),
) = lagVedtak(
    id = id,
    virkningsDato = virkningFom.atDay(1),
    vedtakStatus = VedtakStatus.IVERKSATT,
    behandlingType = behandlingType,
    vedtakFattetDato = vedtakFattetDato,
    utbetalingsperioder =
        listOf(
            Utbetalingsperiode(
                id = id * 10,
                periode = Periode(virkningFom, null),
                beloep = BigDecimal.valueOf(140),
                type = UtbetalingsperiodeType.UTBETALING,
            ),
        ),
)
