package no.nav.etterlatte.regulering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class YtelseIkkeLoependeRiverTest {
//    companion object {
//        val melding =
//            Companion::class.java.getResource("/ytelse_ikke_loepende.json")?.readText()
//                ?: throw FileNotFoundException("Fant ikke filen")
//    }
//
//    private val behandlingService = mockk<BehandlingService>()
//    private val inspector = TestRapid().apply { YtelseIkkeLoependeRiver(this, behandlingService) }
//
//    @Test
//    fun `skal lese melding om ytelse ikke løpende og registrere kjøring`() {
//        every { behandlingService.lagreKjoering(any(), any(), any(), any(), any(), any()) } just Runs
//        inspector.sendTestMessage(melding)
//        verify(exactly = 1) {
//            behandlingService.lagreKjoering(SakId(1005147), KjoeringStatus.IKKE_LOEPENDE, "Regulering-test-2025")
//        }
//    }
}
