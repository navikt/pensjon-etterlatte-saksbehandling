package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class DetaljertBehandlingTest {
    @Test
    fun `uten revurderingsaarsak kan vi innvilge`() {
        Assertions.assertTrue(lagBehandling(null).kanVedta(VedtakType.INNVILGELSE))
    }

    @Test
    fun `med revurderingsaarsak som ikke gir opphoer kan vi vedta endring`() {
        Assertions.assertTrue(lagBehandling(Revurderingaarsak.REGULERING).kanVedta(VedtakType.ENDRING))
    }

    @Test
    fun `med revurderingsaarsak som gir opphoer kan vi vedta opphoer`() {
        Assertions.assertTrue(lagBehandling(Revurderingaarsak.DOEDSFALL).kanVedta(VedtakType.OPPHOER))
    }

    @Test
    fun `med revurderingsaarsak som gir opphoer kan vi ikke vedta endring`() {
        Assertions.assertFalse(lagBehandling(Revurderingaarsak.OMGJOERING_AV_FARSKAP).kanVedta(VedtakType.ENDRING))
    }

    private fun lagBehandling(revurderingsaarsak: Revurderingaarsak?) =
        DetaljertBehandling(
            id = UUID.randomUUID(),
            sak = 1L,
            sakType = SakType.BARNEPENSJON,
            soeker = "123",
            status = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            utlandstilknytning = null,
            revurderingsaarsak = revurderingsaarsak,
            prosesstype = Prosesstype.MANUELL,
            revurderingInfo = null,
            kilde = Vedtaksloesning.GJENNY,
        )
}
