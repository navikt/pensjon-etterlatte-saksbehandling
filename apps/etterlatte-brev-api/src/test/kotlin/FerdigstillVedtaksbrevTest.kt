import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BrevService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import vedtak.VedtakServiceMock
import java.util.*

class FerdigstillVedtaksbrevTest {
    private val service = mockk<BrevService>(relaxed = true)
    private val inspector = TestRapid().apply { FerdigstillVedtaksbrev(this, service) }

    @Test
    fun `Skal ferdigstille vedtaksbrev naar et vedtak blir attestert`() {
        val melding = JsonMessage.newMessage(mapOf(
            eventNameKey to "VEDTAK:ATTESTERT",
            "vedtak" to VedtakServiceMock().hentVedtak("ABCD")
        ))

        inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) { service.ferdigstillAttestertVedtak(any()) }

        confirmVerified(service)
    }
}
