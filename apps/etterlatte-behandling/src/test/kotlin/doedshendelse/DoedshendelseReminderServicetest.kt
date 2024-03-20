package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Status
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Utfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class DoedshendelseReminderServicetest {
    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val dao = mockk<DoedshendelseDao>()
    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>()

    private val dataSource = mockk<DataSource>()

    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(dataSource), mockk())

    @Test
    fun `Skal opprette oppgave hvis 2 mnd gammel BP hendelse ikke har soekt`() {
        val sakId = 1L
        val doedshendelseBP2mndGammel =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            ).copy(endret = LocalDateTime.now().minusMonths(2L).toTidspunkt(), utfall = Utfall.BREV, status = Status.FERDIG, sakId = sakId)

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
        every { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any(), any()) } returns mockOppgave

        val service =
            DoedshendelseReminderService(
                featureToggleService = toggle,
                doedshendelseDao = dao,
                behandlingService = behandlingService,
                oppgaveService = oppgaveService,
            )
        service.setupKontekstAndRun(kontekst)

        verify { behandlingService.hentBehandlingerForSak(sakId) }
        verify { oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any(), any()) }
    }
}
