package behandling

import no.nav.etterlatte.behandling.VedtakService
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class VedtakServiceTest {

    @Test
    fun fattVedtak() {
        val rapid = TestProdusent<String, String>()
        val service = VedtakService(rapid)
        service.fattVedtak("behanldingId", "saksbehandlerIdent")

        assertEquals(1, rapid.publiserteMeldinger.size)
        val publisertMelding = rapid.publiserteMeldinger.first().verdi.let{ objectMapper.readTree(it)}
        assertEquals("SAKSBEHANDLER:FATT_VEDTAK", publisertMelding["@event"].textValue())
        assertEquals("behanldingId", publisertMelding["@behandlingId"].textValue())
        assertEquals("saksbehandlerIdent", publisertMelding["@saksbehandler"].textValue())
    }

    @Test
    fun attesterVedtak() {
        val rapid = TestProdusent<String, String>()
        val service = VedtakService(rapid)
        service.attesterVedtak("behanldingId", "saksbehandlerIdent")

        assertEquals(1, rapid.publiserteMeldinger.size)
        val publisertMelding = rapid.publiserteMeldinger.first().verdi.let{ objectMapper.readTree(it)}
        assertEquals("SAKSBEHANDLER:ATTESTER_VEDTAK", publisertMelding["@event"].textValue())
        assertEquals("behanldingId", publisertMelding["@behandlingId"].textValue())
        assertEquals("saksbehandlerIdent", publisertMelding["@saksbehandler"].textValue())
    }

    @Test
    fun underkjennVedtak() {
        val rapid = TestProdusent<String, String>()
        val service = VedtakService(rapid)
        service.underkjennVedtak("behanldingId", "begrunnelse", "kommentar","saksbehandlerIdent")

        assertEquals(1, rapid.publiserteMeldinger.size)
        val publisertMelding = rapid.publiserteMeldinger.first().verdi.let{ objectMapper.readTree(it)}
        assertEquals("SAKSBEHANDLER:UNDERKJENN_VEDTAK", publisertMelding["@event"].textValue())
        assertEquals("behanldingId", publisertMelding["@behandlingId"].textValue())
        assertEquals("saksbehandlerIdent", publisertMelding["@saksbehandler"].textValue())
        assertEquals("begrunnelse", publisertMelding["@valgtBegrunnelse"].textValue())
        assertEquals("kommentar", publisertMelding["@kommentar"].textValue())
    }
}