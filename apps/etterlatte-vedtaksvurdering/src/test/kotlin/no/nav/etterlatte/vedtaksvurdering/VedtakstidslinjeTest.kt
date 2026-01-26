package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.Regelverk
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
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit
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
        val actual = Vedtakstidslinje(emptyList()).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertNull(actual.sisteLoependeBehandlingId)
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
        val actual = Vedtakstidslinje(listOf(fattetVedtak)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertNull(actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * |-----------------------Iverksatt----------------------------|
     * */
    @Test
    fun `sak med iverksatt foerstegangsbehandling med vedtaksdato foer fraOgMed er loepende`() {
        val sisteLoependeBehandlingId = UUID.randomUUID()

        val iverksattDato =
            lagVedtak(
                id = 1,
                behandlingId = sisteLoependeBehandlingId,
                virkningsDato = LocalDate.of(2023, 1, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
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
            ).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertNull(actual.sisteLoependeBehandlingId)
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
        val sisteLoependeBehandlingId = UUID.randomUUID()

        val iverksattDato =
            lagVedtak(
                id = 1,
                behandlingId = sisteLoependeBehandlingId,
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
            ).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                           |---------Iverksatt----------------|
     * */
    @Test
    fun `sak som er iverksatt maanaden etter fraOgMed-datoen er loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val sisteLoependeBehandlingId = UUID.randomUUID()

        val iverksattDato =
            lagVedtak(
                id = 1,
                behandlingId = sisteLoependeBehandlingId,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
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
        val sisteLoependeBehandlingId = UUID.randomUUID()

        val iverksattDato =
            lagVedtak(
                id = 1,
                behandlingId = sisteLoependeBehandlingId,
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

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
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

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertNull(actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|-----|--*-|----|----|----|----|----|----|
     * |------------Innvilget----------|
     *                           |---Opphør----------------------|
     *                     |-----| Revurdert
     * */
    @Test
    fun `opphør med senere attestasjonsdato på endring(regulering) enn seg selv`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2024, 1, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )

        val endring =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2024, 5, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(2, DAYS),
                opphoerFraOgMed = YearMonth.of(2024, 6),
            )

        val opphoertDato =
            lagVedtak(
                id = 3,
                virkningsDato = LocalDate.of(2024, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(1, DAYS),
                opphoerFraOgMed = YearMonth.of(2024, 6),
            )

        val actual =
            Vedtakstidslinje(listOf(iverksattDato, endring, opphoertDato)).harLoependeVedtakPaaEllerEtter(
                LocalDate.of(
                    2024,
                    6,
                    4,
                ),
            )
        assertEquals(false, actual.erLoepende)
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
        val sisteLoependeBehandlingId = UUID.randomUUID()
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
                behandlingId = sisteLoependeBehandlingId,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     * |-----------------------Iverksatt----------------------------|
     *      |------------------Til samordning-----------------------|
     * */
    @Test
    fun `sak med iverksatt foerstegangsbehandling og revurdering til samordning er løpende og under samordning`() {
        val sisteLoependeBehandlingId = UUID.randomUUID()

        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 1, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
            )
        val vedtakTilSamordning =
            lagVedtak(
                id = 1,
                behandlingId = sisteLoependeBehandlingId,
                virkningsDato = LocalDate.of(2023, 2, 1),
                vedtakStatus = VedtakStatus.TIL_SAMORDNING,
                vedtakType = VedtakType.ENDRING,
            )

        val actual =
            Vedtakstidslinje(listOf(iverksattDato, vedtakTilSamordning)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(true, actual.underSamordning)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                           |---------Iverksatt----------------|
     *                                |---------Opphør--------------|
     *                           |----| Revurdert
     * */
    @Test
    fun `sak som er iverksatt, opphørt og deretter revurdert tilbake i tid etter fraOgMed er loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val sisteLoependeBehandlingId = UUID.randomUUID()
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
        val revurdertDato =
            lagVedtak(
                id = 1,
                behandlingId = sisteLoependeBehandlingId,
                virkningsDato = LocalDate.of(2023, 6, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(2, DAYS),
            )

        val actual =
            Vedtakstidslinje(
                listOf(
                    iverksattDato,
                    opphoertDato,
                    revurdertDato,
                ),
            ).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                |------------Iverksatt------------------------|
     *                     |-------Opphør---------------------------|
     *                |----|       Revurdert
     * */
    @Test
    fun `sak som er iverksatt, opphørt og deretter revurdert tilbake i tid før fraOgMed er ikke loepende`() {
        val attesteringsdato = Tidspunkt.now()
        val opphoer = LocalDate.of(2023, 5, 1)
        val iverksattDato =
            lagVedtak(
                id = 3,
                virkningsDato = LocalDate.of(2023, 4, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val opphoertDato =
            lagVedtak(
                id = 2,
                virkningsDato = opphoer,
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )
        val revurdertDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 4, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(2, DAYS),
                opphoerFraOgMed = YearMonth.from(opphoer),
            )

        val actual =
            Vedtakstidslinje(
                listOf(
                    iverksattDato,
                    opphoertDato,
                    revurdertDato,
                ),
            ).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(fraOgMed, actual.dato)
        assertNull(actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                |------------Iverksatt------------------------|
     *                     |-------Revurdert------------------------|
     *                |------------Opphør---------------------------|
     * */
    @Test
    fun `Opphør tilbake i tid til innvilgelse etter innvilgelse og revurdering skal bli ikke løpende`() {
        val attesteringsdato = Tidspunkt.now()
        val iverksattDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 4, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val revurderingDato =
            lagVedtak(
                id = 2,
                virkningsDato = LocalDate.of(2023, 5, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )
        val opphoerDato =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 4, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(2, DAYS),
            )

        val actual =
            Vedtakstidslinje(listOf(iverksattDato, revurderingDato, opphoerDato)).harLoependeVedtakPaaEllerEtter(
                fraOgMed,
            )
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertNull(actual.sisteLoependeBehandlingId)
    }

    /**
     *   Jan  Feb  Mar  Apr  Mai   Jun  Jul  Aug  Sep  Okt  Nov  Dec
     * |----|----|----|----|--*--|----|----|----|----|----|----|----|
     *                |------------Iverksatt------------------------|
     *                                |-------Revurdert-------------|
     *                                     |-------Opphør-----------|
     * */
    @Test
    fun `Revurdering frem i tid, så opphør skal bli løpende`() {
        val attesteringsdato = Tidspunkt.now()
        val sisteLoependeBehandlingId = UUID.randomUUID()

        val innvilgelse =
            lagVedtak(
                id = 1,
                virkningsDato = LocalDate.of(2023, 4, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.INNVILGELSE,
                datoAttestert = attesteringsdato,
            )
        val revurdering =
            lagVedtak(
                id = 2,
                behandlingId = sisteLoependeBehandlingId,
                virkningsDato = LocalDate.of(2023, 7, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                behandlingType = BehandlingType.REVURDERING,
                vedtakType = VedtakType.ENDRING,
                datoAttestert = attesteringsdato.plus(1, DAYS),
            )
        val opphoer =
            lagVedtak(
                id = 3,
                virkningsDato = LocalDate.of(2023, 8, 1),
                vedtakStatus = VedtakStatus.IVERKSATT,
                vedtakType = VedtakType.OPPHOER,
                datoAttestert = attesteringsdato.plus(2, DAYS),
            )

        val actual =
            Vedtakstidslinje(listOf(innvilgelse, revurdering, opphoer)).harLoependeVedtakPaaEllerEtter(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
        assertEquals(sisteLoependeBehandlingId, actual.sisteLoependeBehandlingId)
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
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
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
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                            Utbetalingsperiode(
                                id = 11,
                                periode = Periode(feb2024, april2024),
                                beloep = BigDecimal.valueOf(150),
                                type = UtbetalingsperiodeType.UTBETALING,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                            Utbetalingsperiode(
                                id = 12,
                                periode = Periode(mai2024, null),
                                beloep = BigDecimal.valueOf(160),
                                type = UtbetalingsperiodeType.UTBETALING,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
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
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
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

        @Test
        fun `innvilgede perioder finner 2 separerte perioder hvis det er opphør imellom`() {
            val vedtakVirkJanuar =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 1,
                    virkningFom = YearMonth.of(2024, Month.JANUARY),
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    vedtakFattetDato = Tidspunkt.now().minus(3, DAYS),
                    opphoerFraOgMed = YearMonth.of(2024, Month.AUGUST),
                )

            val annulererOpphoer =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 2,
                    virkningFom = YearMonth.of(2024, Month.AUGUST),
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.now().minus(2, DAYS),
                )

            val vedtakOpphoerFomSeptember =
                lagVedtak(
                    id = 3,
                    virkningsDato = YearMonth.of(2024, Month.SEPTEMBER).atDay(1),
                    behandlingId = UUID.randomUUID(),
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    datoAttestert = Tidspunkt.now().minus(1, DAYS),
                    vedtakType = VedtakType.OPPHOER,
                    vedtakFattetDato = Tidspunkt.now().minus(1, DAYS),
                    utbetalingsperioder = listOf(),
                    opphoerFraOgMed = null,
                )

            val igangsattIgjen =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 4,
                    virkningFom = YearMonth.of(2024, Month.DECEMBER),
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.now(),
                    opphoerFraOgMed = null,
                )

            val tidslinje =
                Vedtakstidslinje(listOf(vedtakVirkJanuar, annulererOpphoer, vedtakOpphoerFomSeptember, igangsattIgjen))

            val innvilgetPeriode = tidslinje.innvilgedePerioder()
            innvilgetPeriode.size shouldBe 2
            innvilgetPeriode[0].periode.fom shouldBe januar2024
            innvilgetPeriode[0].periode.tom shouldBe YearMonth.of(2024, Month.AUGUST)
            innvilgetPeriode[1].periode.fom shouldBe YearMonth.of(2024, Month.DECEMBER)
            innvilgetPeriode[1].periode.tom shouldBe null
        }

        @Test
        fun `innvilgedePerioder gir riktig svar med kun ett vedtak`() {
            val vedtakVirkJanuar =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 1,
                    virkningFom = YearMonth.of(2024, Month.JANUARY),
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    vedtakFattetDato = Tidspunkt.now().minus(3, DAYS),
                    opphoerFraOgMed = YearMonth.of(2024, Month.AUGUST),
                )

            val tidslinje = Vedtakstidslinje(listOf(vedtakVirkJanuar))
            val innvilgetPeriode = tidslinje.innvilgedePerioder()

            innvilgetPeriode.size shouldBe 1
            innvilgetPeriode[0].periode.fom shouldBe januar2024
            innvilgetPeriode[0].periode.tom shouldBe YearMonth.of(2024, Month.JULY)
        }

        @Test
        fun `innvilgede perioder håndterer opphør fra første virk`() {
            val vedtakVirkJanuar =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 1,
                    virkningFom = YearMonth.of(2024, Month.JANUARY),
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    vedtakFattetDato = Tidspunkt.now().minus(3, DAYS),
                )
            val opphoerFraFoersteVirk =
                lagVedtak(
                    id = 2,
                    virkningsDato = YearMonth.of(2024, Month.JANUARY).atDay(1),
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    datoAttestert = Tidspunkt.now(),
                    vedtakType = VedtakType.OPPHOER,
                    vedtakFattetDato = Tidspunkt.now().minus(1, DAYS),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                id = 3,
                                periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = null),
                                beloep = null,
                                type = UtbetalingsperiodeType.OPPHOER,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                    opphoerFraOgMed = YearMonth.of(2024, Month.JANUARY),
                )

            val tidslinje = Vedtakstidslinje(listOf(vedtakVirkJanuar, opphoerFraFoersteVirk))
            val innvilgetPeriode = tidslinje.innvilgedePerioder()
            innvilgetPeriode.size shouldBe 0
        }

        @Test
        fun `innvilgede perioder håndterer en opphørt start og så en periode innvilget senere`() {
            val vedtakVirkJanuar =
                lagStandardVedtakMedEnAapenUtbetalingsperiode(
                    id = 1,
                    virkningFom = YearMonth.of(2024, Month.JANUARY),
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    vedtakFattetDato = Tidspunkt.now().minus(3, DAYS),
                )
            val opphoerFraFoersteVirk =
                lagVedtak(
                    id = 2,
                    virkningsDato = YearMonth.of(2024, Month.JANUARY).atDay(1),
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    datoAttestert = Tidspunkt.now().minus(1, DAYS),
                    vedtakType = VedtakType.OPPHOER,
                    vedtakFattetDato = Tidspunkt.now().minus(1, DAYS),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                id = 3,
                                periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = null),
                                beloep = null,
                                type = UtbetalingsperiodeType.OPPHOER,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                    opphoerFraOgMed = YearMonth.of(2024, Month.JANUARY),
                )

            val revurderingInnvilgetFraMai =
                lagVedtak(
                    id = 3,
                    virkningsDato = YearMonth.of(2024, Month.MAY).atDay(1),
                    vedtakType = VedtakType.ENDRING,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.now(),
                    datoAttestert = Tidspunkt.now(),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                )

            val tidslinje = Vedtakstidslinje(listOf(vedtakVirkJanuar, opphoerFraFoersteVirk, revurderingInnvilgetFraMai))
            val innvilgetPeriode = tidslinje.innvilgedePerioder()
            innvilgetPeriode.size shouldBe 1
            innvilgetPeriode[0].periode.fom shouldBe YearMonth.of(2024, Month.MAY)
            innvilgetPeriode[0].periode.tom shouldBe null
        }

        @Test
        fun `Skal kunne ha et opphør og en ny revurdering med virk før opphør som bygger opp utbetalingslinjer med opphøret`() {
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
                                periode = Periode(januar2024, null),
                                beloep = BigDecimal.valueOf(140),
                                type = UtbetalingsperiodeType.UTBETALING,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                )

            val opphoerFomMars =
                lagVedtak(
                    id = 2,
                    virkningsDato = mars2024.atDay(1),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.parse("2024-02-02T13:30:00Z"),
                    vedtakType = VedtakType.OPPHOER,
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                periode = Periode(mars2024, null),
                                beloep = null,
                                type = UtbetalingsperiodeType.OPPHOER,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                )

            val vedtakTilbakeITidFomFeb =
                lagVedtak(
                    id = 1,
                    virkningsDato = feb2024.atDay(1),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.parse("2024-03-02T13:30:00Z"),
                    utbetalingsperioder =
                        listOf(
                            Utbetalingsperiode(
                                id = 10,
                                periode = Periode(feb2024, feb2024),
                                beloep = BigDecimal.valueOf(160),
                                type = UtbetalingsperiodeType.UTBETALING,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                            Utbetalingsperiode(
                                periode = Periode(mars2024, null),
                                beloep = null,
                                type = UtbetalingsperiodeType.OPPHOER,
                                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                )

            val sammenstilt =
                Vedtakstidslinje(listOf(vedtakFomJanuar2024, opphoerFomMars, vedtakTilbakeITidFomFeb))
                    .sammenstill(januar2024)

            assertAll(
                { sammenstilt.size shouldBe 2 },
                { sammenstilt[0].utbetalingsperioder.size shouldBe 1 },
                { sammenstilt[0].utbetalingsperioder[0].periode.fom shouldBe januar2024 },
                { sammenstilt[0].utbetalingsperioder[0].periode.tom shouldBe januar2024 },
                { sammenstilt[1].utbetalingsperioder.size shouldBe 2 },
                { sammenstilt[1].utbetalingsperioder[0].periode.fom shouldBe feb2024 },
                { sammenstilt[1].utbetalingsperioder[0].periode.tom shouldBe feb2024 },
                { sammenstilt[1].utbetalingsperioder[1].periode.fom shouldBe mars2024 },
                { sammenstilt[1].utbetalingsperioder[1].periode.tom shouldBe null },
                { sammenstilt[1].utbetalingsperioder[1].type shouldBe UtbetalingsperiodeType.OPPHOER },
            )
        }

        @Test
        fun `skal kun bruke vedtak med innhold type Behandling og ignorere Tilbakekreving`() {
            val januar2024 = YearMonth.of(2024, Month.JANUARY)
            val behandlingVedtak =
                lagVedtak(
                    id = 1,
                    virkningsDato = januar2024.atDay(1),
                    vedtakStatus = VedtakStatus.IVERKSATT,
                    behandlingType = BehandlingType.REVURDERING,
                    vedtakFattetDato = Tidspunkt.now(),
                    utbetalingsperioder = listOf(),
                )

            val tilbakekrevingVedtak =
                Vedtak(
                    id = 2,
                    sakId = sakId1,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    behandlingId = UUID.randomUUID(),
                    soeker = SOEKER_FOEDSELSNUMMER,
                    status = VedtakStatus.IVERKSATT,
                    type = VedtakType.TILBAKEKREVING,
                    vedtakFattet = behandlingVedtak.vedtakFattet,
                    attestasjon = behandlingVedtak.attestasjon,
                    innhold = VedtakInnhold.Tilbakekreving(mockk(relaxed = true)),
                )

            val tidslinje = Vedtakstidslinje(listOf(behandlingVedtak, tilbakekrevingVedtak))
            val sammenstilt = tidslinje.sammenstill(januar2024)

            assertAll(
                { sammenstilt.size shouldBe 1 },
                { (sammenstilt[0].innhold is VedtakInnhold.Behandling) shouldBe true },
            )
        }

        private val Vedtak.utbetalingsperioder: List<Utbetalingsperiode>
            get() = (this.innhold as VedtakInnhold.Behandling).utbetalingsperioder
    }
}

internal fun lagVedtak(
    id: Long,
    virkningsDato: LocalDate,
    behandlingId: UUID = UUID.randomUUID(),
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    vedtakStatus: VedtakStatus,
    datoAttestert: Tidspunkt = Tidspunkt.now(),
    vedtakType: VedtakType = VedtakType.INNVILGELSE,
    vedtakFattetDato: Tidspunkt = Tidspunkt.now(),
    utbetalingsperioder: List<Utbetalingsperiode> = emptyList(),
    opphoerFraOgMed: YearMonth? = null,
): Vedtak =
    Vedtak(
        id = id,
        sakId = sakId1,
        sakType = SakType.BARNEPENSJON,
        behandlingId = behandlingId,
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
            VedtakInnhold.Behandling(
                virkningstidspunkt = virkningsDato.let { YearMonth.from(it) },
                behandlingType = behandlingType,
                beregning = null,
                avkorting = null,
                vilkaarsvurdering = null,
                utbetalingsperioder = utbetalingsperioder,
                revurderingAarsak = null,
                opphoerFraOgMed =
                    when (opphoerFraOgMed) {
                        null -> if (vedtakType == VedtakType.OPPHOER) YearMonth.from(virkningsDato) else null
                        else -> opphoerFraOgMed
                    },
            ),
    )

private fun lagStandardVedtakMedEnAapenUtbetalingsperiode(
    id: Long,
    virkningFom: YearMonth,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    vedtakFattetDato: Tidspunkt = Tidspunkt.ofNorskTidssone(virkningFom.atDay(1), tid = LocalTime.NOON),
    opphoerFraOgMed: YearMonth? = null,
) = lagVedtak(
    id = id,
    virkningsDato = virkningFom.atDay(1),
    vedtakStatus = VedtakStatus.IVERKSATT,
    behandlingType = behandlingType,
    vedtakFattetDato = vedtakFattetDato,
    datoAttestert = vedtakFattetDato.plus(2, ChronoUnit.MINUTES),
    opphoerFraOgMed = opphoerFraOgMed,
    utbetalingsperioder =
        listOf(
            Utbetalingsperiode(
                id = id * 10,
                periode = Periode(virkningFom, opphoerFraOgMed),
                beloep = BigDecimal.valueOf(140),
                type = UtbetalingsperiodeType.UTBETALING,
                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
            ),
        ),
)
