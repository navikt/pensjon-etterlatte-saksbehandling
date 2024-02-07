package no.nav.etterlatte.behandling.sjekkliste

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.util.UUID

class SjekklisteServiceTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val sjekklisteDao = mockk<SjekklisteDao>()
    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService, oppgaveService)

    @BeforeEach
    fun setup() {
        settOppKontekst(user)
    }

    @Test
    fun `ikke tilgang til aa opprette eller oppdatere sjekkliste hvis behandling ikke er i endre-tilstand`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            foerstegangsbehandling(id = behandlingId, sakId = 1L, status = BehandlingStatus.ATTESTERT)

        assertAll(
            {
                assertThrows<SjekklisteIkkeTillattException> {
                    sjekklisteService.opprettSjekkliste(behandlingId)
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
            foerstegangsbehandling(id = behandlingId, sakId = 33L, status = BehandlingStatus.ATTESTERT)

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
            foerstegangsbehandling(id = behandlingId, sakId = 33L, status = BehandlingStatus.FATTET_VEDTAK)
        every { user.name() } returns "Sak B. Handlersen"
        every {
            oppgaveService.hentSaksbehandlerForOppgaveUnderArbeidByReferanse(behandlingId.toString())
        } returns OppgaveSaksbehandler("Noe helt annet")

        assertThrows<SjekklisteIkkeTillattException> {
            sjekklisteService.oppdaterSjekkliste(
                behandlingId = behandlingId,
                oppdaterSjekkliste = OppdatertSjekkliste(kommentar = "Testing.. 1, 2, 3", versjon = 3),
            )
        }
    }

    private fun settOppKontekst(user: SaksbehandlerMedEnheterOgRoller) {
        Kontekst.set(
            Context(
                user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                },
            ),
        )
    }
}
