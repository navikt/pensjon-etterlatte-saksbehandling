package no.nav.etterlatte.brev.oppgave

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.behandlingklient.OppgaveKlient
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Test
import java.util.UUID

class OppgaveServiceTest {
    private val oppgaveKlient =
        mockk<OppgaveKlient> {
            coEvery { hentOppgaverForSak(any(), any()) } returns listOf(oppgave())
            coEvery { opprettOppgave(any(), any(), any()) } returns oppgave()
        }

    private val oppgaveService = OppgaveService(oppgaveKlient)

    private val bruker = simpleSaksbehandler("Z123456")

    @Test
    fun `opprettOppgaveForFeiletBrev skal opprette en oppgave hvis det ikke finnes en for det feilede brevet`() {
        coEvery { oppgaveKlient.hentOppgaverForSak(any(), any()) } returns emptyList()

        runBlocking {
            oppgaveService.opprettOppgaveForFeiletBrev(SakId(176_167), 2, bruker)
        }

        coVerify(exactly = 1) { oppgaveKlient.opprettOppgave(any(), any(), bruker) }
    }

    @Test
    fun `opprettOppgaveForFeiletBrev skal ikke opprette en oppgave hvis det allerede finnes en for det feilede brevet`() {
        val brevID: Long = 1969
        val sakId = SakId(176_167)

        coEvery { oppgaveKlient.hentOppgaverForSak(any(), any()) } returns
            listOf(
                oppgave(
                    referanse = brevID.toString(),
                    sakId = sakId,
                    type = OppgaveType.MANUELL_UTSENDING_BREV,
                    kilde = OppgaveKilde.DOEDSHENDELSE,
                ),
            )

        runBlocking {
            oppgaveService.opprettOppgaveForFeiletBrev(sakId, brevID, bruker)
        }

        coVerify(exactly = 0) { oppgaveKlient.opprettOppgave(any(), any(), bruker) }
    }

    private fun oppgave(
        id: UUID = UUID.randomUUID(),
        status: Status = no.nav.etterlatte.libs.common.oppgave.Status.NY,
        enhet: Enhetsnummer = Enhetsnummer("1234"),
        sakId: SakId = SakId(1),
        kilde: OppgaveKilde? = OppgaveKilde.HENDELSE,
        type: OppgaveType = OppgaveType.AKTIVITETSPLIKT,
        saksbehandler: OppgaveSaksbehandler? = null,
        forrigeSaksbehandlerIdent: String? = null,
        referanse: String = UUID.randomUUID().toString(),
        gruppeId: String? = null,
        merknad: String? = null,
        opprettet: Tidspunkt = Tidspunkt.now(),
        sakType: SakType = SakType.OMSTILLINGSSTOENAD,
        fnr: String? = "09438336165",
        frist: Tidspunkt? = null,
    ) = OppgaveIntern(
        id,
        status,
        enhet,
        sakId,
        kilde,
        type,
        saksbehandler,
        forrigeSaksbehandlerIdent,
        referanse,
        gruppeId,
        merknad,
        opprettet,
        sakType,
        fnr,
        frist,
    )
}
