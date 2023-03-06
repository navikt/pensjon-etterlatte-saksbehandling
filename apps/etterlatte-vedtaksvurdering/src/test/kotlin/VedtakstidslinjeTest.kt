
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.tilInstant
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.Vedtakstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
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
            vedtakStatus = VedtakStatus.IVERKSATT
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
        val attesteringsdato = Tidspunkt.now().toLocalDatetimeUTC()
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 1, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            datoAttestert = attesteringsdato.tilInstant()
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 4, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            datoAttestert = attesteringsdato.plusDays(1).tilInstant()
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
        val attesteringsdato = Tidspunkt.now().toLocalDatetimeUTC()
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 1, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            datoAttestert = attesteringsdato.tilInstant()
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            datoAttestert = attesteringsdato.plusDays(1).tilInstant()
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
        val attesteringsdato = Tidspunkt.now().toLocalDatetimeUTC()
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            datoAttestert = attesteringsdato.tilInstant()
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
        val attesteringsdato = Tidspunkt.now().toLocalDatetimeUTC()
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            datoAttestert = attesteringsdato.tilInstant()
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 7, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            datoAttestert = attesteringsdato.plusDays(1).tilInstant()
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
        val attesteringsdato = Tidspunkt.now().toLocalDatetimeUTC()
        val iverksattDato = lagVedtak(
            id = 1,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            datoAttestert = attesteringsdato.tilInstant()
        )
        val opphoertDato = lagVedtak(
            id = 2,
            virkningsDato = LocalDate.of(2023, 6, 1),
            vedtakStatus = VedtakStatus.IVERKSATT,
            behandlingType = BehandlingType.REVURDERING,
            datoAttestert = attesteringsdato.plusDays(1).tilInstant()
        )

        val actual = Vedtakstidslinje(listOf(iverksattDato, opphoertDato)).erLoependePaa(fraOgMed)
        assertEquals(false, actual.erLoepende)
        assertEquals(LocalDate.of(2023, 5, 1), actual.dato)
    }
}

private fun lagVedtak(
    id: Long,
    virkningsDato: LocalDate,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    vedtakStatus: VedtakStatus,
    datoAttestert: Instant = Instant.now()
): Vedtak {
    return Vedtak(
        id = id,
        sakId = 1L,
        sakType = SakType.BARNEPENSJON,
        behandlingId = UUID.randomUUID(),
        saksbehandlerId = "saksbehandler01",
        beregningsResultat = null,
        vilkaarsResultat = null,
        vedtakFattet = null,
        fnr = null,
        datoFattet = null,
        datoattestert = datoAttestert,
        attestant = null,
        virkningsDato = virkningsDato,
        vedtakStatus = vedtakStatus,
        behandlingType = behandlingType,
        attestertVedtakEnhet = null,
        fattetVedtakEnhet = null
    )
}