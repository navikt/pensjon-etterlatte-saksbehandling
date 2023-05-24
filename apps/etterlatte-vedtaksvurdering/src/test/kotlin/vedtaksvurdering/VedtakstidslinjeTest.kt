package vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.Vedtakstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit.DAYS
import java.util.*

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
        val fattetVedtak = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 1, 1),
            vedtakStatus = VedtakStatus.FATTET_VEDTAK
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 1, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 1, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE,
            datoAttestert = attesteringsdato
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 4, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            vedtakType = VedtakType.OPPHOER,
            datoAttestert = attesteringsdato.plus(1, DAYS)
        )

        val actual = Vedtakstidslinje(
            listOf(
                iverksattDato,
                opphoertDato
            )
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 1, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE,
            datoAttestert = attesteringsdato
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            vedtakType = VedtakType.OPPHOER,
            datoAttestert = attesteringsdato.plus(1, DAYS)
        )

        val actual = Vedtakstidslinje(
            listOf(
                iverksattDato,
                opphoertDato
            )
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE,
            datoAttestert = attesteringsdato
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE,
            datoAttestert = attesteringsdato
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 7, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            vedtakType = VedtakType.OPPHOER,
            datoAttestert = attesteringsdato.plus(1, DAYS)
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE,
            datoAttestert = attesteringsdato
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            vedtakType = VedtakType.OPPHOER,
            datoAttestert = attesteringsdato.plus(1, DAYS)
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
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            vedtakType = VedtakType.INNVILGELSE,
            datoAttestert = attesteringsdato
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            vedtakType = VedtakType.ENDRING,
            datoAttestert = attesteringsdato.plus(1, DAYS)
        )

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).erLoependePaa(fraOgMed)
        assertEquals(true, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 6, 1), actual.dato)
    }
}

private fun lagVedtak(
    id: Long,
    virkningsDato: LocalDate,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    vedtakStatus: VedtakStatus,
    datoAttestert: Tidspunkt = Tidspunkt.now(),
    vedtakType: VedtakType = VedtakType.INNVILGELSE
): Vedtak {
    return Vedtak(
        id = id,
        sakId = 1L,
        sakType = SakType.BARNEPENSJON,
        behandlingId = UUID.randomUUID(),
        beregning = null,
        avkorting = null,
        vilkaarsvurdering = null,
        soeker = Folkeregisteridentifikator.of(FNR_1),
        virkningstidspunkt = virkningsDato.let { YearMonth.from(it) },
        status = vedtakStatus,
        type = vedtakType,
        behandlingType = behandlingType,
        vedtakFattet = VedtakFattet(
            ansvarligSaksbehandler = SAKSBEHANDLER_1,
            ansvarligEnhet = ENHET_1,
            tidspunkt = Tidspunkt.now()
        ),
        attestasjon = Attestasjon(
            attestant = SAKSBEHANDLER_2,
            attesterendeEnhet = ENHET_2,
            tidspunkt = datoAttestert
        ),
        utbetalingsperioder = emptyList(),
        revurderingAarsak = null
    )
}