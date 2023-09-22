package no.nav.etterlatte.behandling

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.UtenlandstilsnittType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingStatusServiceTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(
                        gjenbruk: Boolean,
                        block: () -> T,
                    ): T {
                        return block()
                    }
                },
            ),
        )
    }

    @Test
    fun `iverksettNasjonal behandling`() {
        val sakId = 1L
        val behandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val behandlingId = behandling.id
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), "sbl")

        val behandlingdao =
            mockk<BehandlingDao> {
                every { lagreStatus(any(), any(), any()) } just runs
            }
        val behandlingService =
            mockk<BehandlingService> {
                every { registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT) } just runs
                every { hentBehandling(behandlingId) } returns behandling
            }
        val grlService = mockk<GrunnlagsendringshendelseService>()
        val oppgaveService = mockk<OppgaveService>()
        val featureToggleService =
            mockk<FeatureToggleService> {
                every { isEnabled(any(), any()) } returns true
            }
        val generellBehandlingService = mockk<GenerellBehandlingService>()

        val sut =
            BehandlingStatusServiceImpl(
                behandlingdao,
                behandlingService,
                grlService,
                oppgaveService,
                featureToggleService,
                generellBehandlingService,
            )

        sut.settIverksattVedtak(behandlingId, iverksettVedtak)

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.IVERKSATT, any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
        }
        confirmVerified(behandlingdao, behandlingService, grlService, oppgaveService)
    }

    @Test
    fun `iverksett utlandstilsnitt behandling`() {
        val sakId = 1L
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                status = BehandlingStatus.ATTESTERT,
                utenlandstilsnitt =
                    Utenlandstilsnitt(
                        UtenlandstilsnittType.UTLANDSTILSNITT,
                        Grunnlagsopplysning.Saksbehandler("ident", Tidspunkt.now()),
                        "begrunnelse",
                    ),
            )
        val behandlingId = behandling.id
        val saksbehandler = "sbl"
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), saksbehandler)

        val behandlingdao =
            mockk<BehandlingDao> {
                every { lagreStatus(any(), any(), any()) } just runs
            }
        val behandlingService =
            mockk<BehandlingService> {
                every { registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT) } just runs
                every { hentBehandling(behandlingId) } returns behandling
            }
        val grlService = mockk<GrunnlagsendringshendelseService>()
        val oppgave =
            OppgaveIntern(
                id = UUID.randomUUID(),
                status = Status.NY,
                enhet = Enheter.defaultEnhet.enhetNr,
                sakId = sakId,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.UTLAND,
                saksbehandler = saksbehandler,
                referanse = behandlingId.toString(),
                merknad = null,
                opprettet = Tidspunkt.now(),
                sakType = SakType.BARNEPENSJON,
                fnr = "123",
                frist = Tidspunkt.now(),
            )
        val oppgaveService =
            mockk<OppgaveService> {
                every {
                    opprettNyOppgaveMedSakOgReferanse(
                        behandlingId.toString(),
                        sakId,
                        OppgaveKilde.BEHANDLING,
                        OppgaveType.UTLAND,
                        null,
                    )
                } returns oppgave
                every { tildelSaksbehandler(oppgave.id, saksbehandler) } just runs
                every { hentSaksbehandlerFraFoerstegangsbehandling(behandlingId) } returns saksbehandler
            }

        val featureToggleService =
            mockk<FeatureToggleService> {
                every { isEnabled(any(), any()) } returns true
            }
        val generellBehandlingService =
            mockk<GenerellBehandlingService> {
                every { opprettBehandling(any()) } just runs
            }

        val sut =
            BehandlingStatusServiceImpl(
                behandlingdao,
                behandlingService,
                grlService,
                oppgaveService,
                featureToggleService,
                generellBehandlingService,
            )

        sut.settIverksattVedtak(behandlingId, iverksettVedtak)

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.IVERKSATT, any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                behandlingId.toString(),
                sakId,
                OppgaveKilde.BEHANDLING,
                OppgaveType.UTLAND,
                null,
            )
            oppgaveService.hentSaksbehandlerFraFoerstegangsbehandling(behandlingId)
            oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler)
            generellBehandlingService.opprettBehandling(any())
        }
        confirmVerified(behandlingdao, behandlingService, grlService, oppgaveService, generellBehandlingService)
    }
}
