package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminder
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

class DoedshendelseReminderServiceTest {
    private val dao = mockk<DoedshendelseDao>()
    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>()

    private val dataSource = mockk<DataSource>()

    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(dataSource), mockk(), null)
    private val antallMaaneder = 4L

    @Test
    fun `Skal opprette oppgave hvis 2 mnd gammel BP hendelse ikke har soekt`() {
        val doedshendelseBP =
            DoedshendelseReminder(
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endret = LocalDateTime.now().minusMonths(antallMaaneder).toNorskTidspunkt(),
                sakId = sakId1,
            )

        val mockOppgave =
            opprettNyOppgaveMedReferanseOgSak(
                "behandling",
                Sak("ident", SakType.BARNEPENSJON, sakId1, Enheter.AALESUND.enhetNr),
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        every { dao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp() } returns listOf(doedshendelseBP)
        every { behandlingService.hentBehandlingerForSak(sakId1) } returns emptyList()
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) } returns mockOppgave
        every { oppgaveService.hentOppgaverForSak(sakId1, OppgaveType.MANGLER_SOEKNAD) } returns emptyList()

        val service =
            DoedshendelseReminderService(
                doedshendelseDao = dao,
                behandlingService = behandlingService,
                oppgaveService = oppgaveService,
            )
        service.setupKontekstAndRun(kontekst)

        verify(exactly = 1) {
            behandlingService.hentBehandlingerForSak(sakId1)
            oppgaveService.hentOppgaverForSak(sakId1, OppgaveType.MANGLER_SOEKNAD)

            oppgaveService.opprettOppgave(
                doedshendelseBP.id.toString(),
                sakId1,
                OppgaveKilde.DOEDSHENDELSE,
                OppgaveType.MANGLER_SOEKNAD,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Skal ikke opprette oppgave hvis 2 mnd gammel BP hendelse ikke har soekt og det allerede er opprettet`() {
        val doedshendelseBP =
            DoedshendelseReminder(
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endret = LocalDateTime.now().minusMonths(antallMaaneder).toNorskTidspunkt(),
                sakId = sakId1,
            )

        val eksisterendeOppgave =
            opprettNyOppgaveMedReferanseOgSak(
                "vurder konsekvens",
                Sak("ident", SakType.BARNEPENSJON, sakId1, Enheter.AALESUND.enhetNr),
                OppgaveKilde.BEHANDLING,
                OppgaveType.MANGLER_SOEKNAD,
                null,
            )
        every { dao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp() } returns listOf(doedshendelseBP)
        every { behandlingService.hentBehandlingerForSak(sakId1) } returns emptyList()
        every { oppgaveService.hentOppgaverForSak(sakId1, OppgaveType.MANGLER_SOEKNAD) } returns listOf(eksisterendeOppgave)

        val service =
            DoedshendelseReminderService(
                doedshendelseDao = dao,
                behandlingService = behandlingService,
                oppgaveService = oppgaveService,
            )
        service.setupKontekstAndRun(kontekst)

        verify { behandlingService.hentBehandlingerForSak(sakId1) }
        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) }
    }
}
