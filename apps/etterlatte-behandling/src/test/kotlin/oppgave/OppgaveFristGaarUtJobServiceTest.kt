package no.nav.etterlatte.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUt
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class OppgaveFristGaarUtJobServiceTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val service = OppgaveFristGaarUtJobService(oppgaveService)
    private val dataSource = mockk<DataSource>()
    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(dataSource), mockk(), null)

    @Test
    fun `Skal klare å ta oppgave av vent hvis frist har gått ut`() {
        val sakId = sakId1
        val fristUte = VentefristGaarUt(sakId, "referanse", UUID.randomUUID(), OppgaveKilde.BEHANDLING, "merk")
        every { oppgaveService.hentFristGaarUt(any()) } returns listOf(fristUte)
        every {
            oppgaveService.endrePaaVent(fristUte.oppgaveId, any(), any(), any())
        } returns mockk()
        service.setupKontekstAndRun(kontekst)
        verify(exactly = 1) { oppgaveService.hentFristGaarUt(any()) }
        verify(exactly = 1) {
            oppgaveService.endrePaaVent(fristUte.oppgaveId, any(), any(), any())
        }
    }

    @Test
    fun `Skal klare å ta neste av vent om en tryner`() {
        val sakId = sakId1
        val fristUte = VentefristGaarUt(sakId, "referanse", UUID.randomUUID(), OppgaveKilde.BEHANDLING, "merk")
        val fristUteMenFaarIkkeOppdatertStatusOgMerknad =
            VentefristGaarUt(sakId, "referanse", UUID.randomUUID(), OppgaveKilde.BEHANDLING, "feil")
        every { oppgaveService.hentFristGaarUt(any()) } returns
            listOf(
                fristUteMenFaarIkkeOppdatertStatusOgMerknad,
                fristUte,
            )
        every {
            oppgaveService.endrePaaVent(
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.oppgaveId,
                any(),
                any(),
                any(),
            )
        } throws
            OppgaveKanIkkeEndres(
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.oppgaveId,
                Status.UNDER_BEHANDLING,
            )
        every { oppgaveService.endrePaaVent(fristUte.oppgaveId, any(), any(), any()) } returns mockk()

        service.setupKontekstAndRun(kontekst)
        verify(exactly = 1) { oppgaveService.hentFristGaarUt(any()) }
        verify(exactly = 1) { oppgaveService.endrePaaVent(fristUte.oppgaveId, any(), any(), any()) }
        verify(exactly = 1) {
            oppgaveService.endrePaaVent(
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.oppgaveId,
                any(),
                any(),
                any(),
            )
        }
    }
}
