package no.nav.etterlatte.oppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Self
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
        val sakId = 1L
        val fristUte = VentefristGaarUt(sakId, "referanse", UUID.randomUUID(), OppgaveKilde.BEHANDLING, "merk")
        every { oppgaveService.hentFristGaarUt(any()) } returns listOf(fristUte)
        every { oppgaveService.oppdaterStatusOgMerknad(fristUte.oppgaveId, fristUte.merknad!!, Status.UNDER_BEHANDLING) } just runs

        service.setupKontekstAndRun(kontekst)
        verify(exactly = 1) { oppgaveService.hentFristGaarUt(any()) }
        verify(exactly = 1) { oppgaveService.oppdaterStatusOgMerknad(fristUte.oppgaveId, "merk", Status.UNDER_BEHANDLING) }
    }

    @Test
    fun `Skal klare å ta neste av vent om en tryner`() {
        val sakId = 1L
        val fristUte = VentefristGaarUt(sakId, "referanse", UUID.randomUUID(), OppgaveKilde.BEHANDLING, "merk")
        val fristUteMenFaarIkkeOppdatertStatusOgMerknad =
            VentefristGaarUt(sakId, "referanse", UUID.randomUUID(), OppgaveKilde.BEHANDLING, "feil")
        every { oppgaveService.hentFristGaarUt(any()) } returns listOf(fristUte)
        every { oppgaveService.oppdaterStatusOgMerknad(fristUte.oppgaveId, fristUte.merknad!!, Status.UNDER_BEHANDLING) } just runs
        every {
            oppgaveService.oppdaterStatusOgMerknad(
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.oppgaveId,
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.merknad!!,
                Status.UNDER_BEHANDLING,
            )
        } throws OppgaveKanIkkeEndres(fristUteMenFaarIkkeOppdatertStatusOgMerknad.oppgaveId, Status.UNDER_BEHANDLING)

        service.setupKontekstAndRun(kontekst)
        verify(exactly = 1) { oppgaveService.hentFristGaarUt(any()) }
        verify(exactly = 1) { oppgaveService.oppdaterStatusOgMerknad(fristUte.oppgaveId, fristUte.merknad!!, Status.UNDER_BEHANDLING) }
        verify(exactly = 0) {
            oppgaveService.oppdaterStatusOgMerknad(
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.oppgaveId,
                fristUteMenFaarIkkeOppdatertStatusOgMerknad.merknad!!,
                Status.UNDER_BEHANDLING,
            )
        }
    }
}
