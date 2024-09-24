package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminder
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
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
    fun `Skal opprette oppgave hvis 4 mnd gammel BP hendelse ikke har soekt`() {
        val sakId = 1L
        val doedshendelseBP2mndGammel =
            DoedshendelseReminder(
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endret = LocalDateTime.now().minusMonths(antallMaaneder).toNorskTidspunkt(),
                sakId = sakId,
            )

        val mockOppgave =
            opprettNyOppgaveMedReferanseOgSak(
                "behandling",
                Sak("ident", SakType.BARNEPENSJON, sakId, Enheter.AALESUND.enhetNr),
                OppgaveKilde.BEHANDLING,
                OppgaveType.FOERSTEGANGSBEHANDLING,
                null,
            )
        every { dao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp() } returns listOf(doedshendelseBP2mndGammel)
        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) } returns mockOppgave
        every { oppgaveService.hentOppgaverForSak(sakId) } returns emptyList()

        val service =
            DoedshendelseReminderService(
                doedshendelseDao = dao,
                behandlingService = behandlingService,
                oppgaveService = oppgaveService,
            )
        service.setupKontekstAndRun(kontekst)

        verify(exactly = 1) {
            behandlingService.hentBehandlingerForSak(sakId)
            oppgaveService.hentOppgaverForSak(sakId)

            oppgaveService.opprettOppgave(
                doedshendelseBP2mndGammel.id.toString(),
                sakId,
                OppgaveKilde.DOEDSHENDELSE,
                OppgaveType.MANGLER_SOEKNAD,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Skal ikke opprette oppgave hvis 4 mnd gammel BP hendelse ikke har soekt og det allerede er opprettet`() {
        val sakId = 1L
        val doedshendelseBP2mndGammel =
            DoedshendelseReminder(
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endret = LocalDateTime.now().minusMonths(antallMaaneder).toNorskTidspunkt(),
                sakId = sakId,
            )

        val eksisterendeOppgave =
            opprettNyOppgaveMedReferanseOgSak(
                "vurder konsekvens",
                Sak("ident", SakType.BARNEPENSJON, sakId, Enheter.AALESUND.enhetNr),
                OppgaveKilde.BEHANDLING,
                OppgaveType.MANGLER_SOEKNAD,
                null,
            )
        every { dao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp() } returns listOf(doedshendelseBP2mndGammel)
        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        every { oppgaveService.hentOppgaverForSak(sakId) } returns listOf(eksisterendeOppgave)

        val service =
            DoedshendelseReminderService(
                doedshendelseDao = dao,
                behandlingService = behandlingService,
                oppgaveService = oppgaveService,
            )
        service.setupKontekstAndRun(kontekst)

        verify { behandlingService.hentBehandlingerForSak(sakId) }
        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) }
    }
}
