package behandling.sjekkliste

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.sjekkliste.OppdaterSjekkliste
import no.nav.etterlatte.behandling.sjekkliste.OppdaterSjekklisteItem
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteDao
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class SjekklisteServiceTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val sjekklisteDao = mockk<SjekklisteDao>()
    private val behandlingService = mockk<BehandlingService>()
    private val sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService)

    @BeforeEach
    fun setup() {
        settOppKontekst(user)
    }

    @Test
    fun `ikke tilgang til aa opprette eller oppdatere sjekkliste hvis behandling ikke er i endre-tilstand`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            foerstegangsbehandling(id = behandlingId, sakId = 1L, status = BehandlingStatus.FATTET_VEDTAK)

        assertAll(
            {
                assertThrows<IllegalStateException> {
                    sjekklisteService.opprettSjekkliste(behandlingId)
                }
            },
            {
                assertThrows<IllegalStateException> {
                    sjekklisteService.oppdaterSjekkliste(behandlingId, OppdaterSjekkliste())
                }
            },
        )
    }

    @Test
    fun `ikke tilgang til aa endre data paa sjekkliste hvis behandling ikke er i endre-tilstand`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            foerstegangsbehandling(id = behandlingId, sakId = 33L, status = BehandlingStatus.FATTET_VEDTAK)

        assertThrows<IllegalStateException> {
            sjekklisteService.oppdaterSjekklisteItem(
                behandlingId = behandlingId,
                itemId = 123L,
                oppdatering = OppdaterSjekklisteItem(avkrysset = true, versjon = 5),
            )
        }
    }
}
