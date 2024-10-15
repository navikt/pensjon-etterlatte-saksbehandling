package no.nav.etterlatte.behandling.sjekkliste

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoServiceTest.Companion.bruker
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class SjekklisteServiceTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val sjekklisteDao = mockk<SjekklisteDao>()
    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService, oppgaveService, grunnlagKlient)

    @BeforeEach
    fun setup() {
        nyKontekstMedBruker(user.also { every { it.name() } returns this::class.java.simpleName })
    }

    @Test
    fun `ikke tilgang til aa opprette eller oppdatere sjekkliste hvis behandling ikke er i endre-tilstand`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            foerstegangsbehandling(id = behandlingId, sakId = sakId1, status = BehandlingStatus.ATTESTERT)

        assertAll(
            {
                assertThrows<SjekklisteIkkeTillattException> {
                    runBlocking {
                        sjekklisteService.opprettSjekkliste(behandlingId, bruker)
                    }
                }
            },
            {
                assertThrows<SjekklisteIkkeTillattException> {
                    sjekklisteService.oppdaterSjekkliste(
                        behandlingId,
                        OppdatertSjekkliste(
                            kommentar = "Lorem ipsum",
                            versjon = 3,
                        ),
                    )
                }
            },
        )
    }

    @Test
    fun `ikke tilgang til aa endre data paa sjekkliste hvis behandling ikke er i endre-tilstand`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            foerstegangsbehandling(id = behandlingId, sakId = randomSakId(), status = BehandlingStatus.ATTESTERT)

        assertThrows<SjekklisteIkkeTillattException> {
            sjekklisteService.oppdaterSjekklisteItem(
                behandlingId = behandlingId,
                itemId = 123L,
                oppdatering = OppdaterSjekklisteItem(avkrysset = true, versjon = 5),
            )
        }
    }

    @Test
    fun `ikke tilgang til aa endre sjekkliste hvis vedtak er fattet, men oppgave er ikke tildelt aktuell saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            foerstegangsbehandling(id = behandlingId, sakId = randomSakId(), status = BehandlingStatus.FATTET_VEDTAK)
        every { user.name() } returns "Sak B. Handlersen"
        every {
            oppgaveService.hentOppgaveUnderBehandling(behandlingId.toString())
        } returns
            mockk<OppgaveIntern> {
                every { saksbehandler } returns OppgaveSaksbehandler("Noe helt annet")
            }

        assertThrows<SjekklisteIkkeTillattException> {
            sjekklisteService.oppdaterSjekkliste(
                behandlingId = behandlingId,
                oppdaterSjekkliste = OppdatertSjekkliste(kommentar = "Testing.. 1, 2, 3", versjon = 3),
            )
        }
    }
}
