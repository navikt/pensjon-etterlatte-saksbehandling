package no.nav.etterlatte.behandling

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingStatusServiceTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeEach
    fun before() {
        nyKontekstMedBruker(user)
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
        val generellBehandlingService = mockk<GenerellBehandlingService>()

        val sut =
            BehandlingStatusServiceImpl(
                behandlingdao,
                behandlingService,
                grlService,
                generellBehandlingService,
            )

        inTransaction {
            sut.settIverksattVedtak(behandlingId, iverksettVedtak)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.IVERKSATT, any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
        }
        confirmVerified(behandlingdao, behandlingService, grlService)
    }

    @Test
    fun `iverksett utlandstilsnitt behandling`() {
        val sakId = 1L
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                status = BehandlingStatus.ATTESTERT,
                boddEllerArbeidetUtlandet =
                    BoddEllerArbeidetUtlandet(
                        boddEllerArbeidetUtlandet = true,
                        skalSendeKravpakke = true,
                        begrunnelse = "beg",
                        kilde = Grunnlagsopplysning.Saksbehandler.create("navIdent"),
                    ),
            )
        val behandlingId = behandling.id
        val saksbehandler = "sbl"
        val iverksattVedtak = VedtakHendelse(1L, Tidspunkt.now(), saksbehandler)

        val behandlingdao =
            mockk<BehandlingDao> {
                every { lagreStatus(any(), any(), any()) } just runs
            }
        val behandlingService =
            mockk<BehandlingService> {
                every { registrerVedtakHendelse(behandlingId, iverksattVedtak, HendelseType.IVERKSATT) } just runs
                every { hentBehandling(behandlingId) } returns behandling
            }
        val grlService = mockk<GrunnlagsendringshendelseService>()

        val generellBehandlingUtland =
            GenerellBehandling.opprettUtland(
                sakId,
                behandlingId,
            )
        val generellBehandlingService =
            mockk<GenerellBehandlingService> {
                every { opprettBehandling(any(), any()) } returns generellBehandlingUtland
            }

        val sut =
            BehandlingStatusServiceImpl(
                behandlingdao,
                behandlingService,
                grlService,
                generellBehandlingService,
            )

        inTransaction {
            sut.settIverksattVedtak(behandlingId, iverksattVedtak)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.IVERKSATT, any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksattVedtak, HendelseType.IVERKSATT)
            generellBehandlingService.opprettBehandling(any(), any())
        }
        confirmVerified(behandlingdao, behandlingService, grlService, generellBehandlingService)
    }

    @Test
    fun `utlandstilsnitt avslag behandling ikke oppfylt vilkårsvurdering`() {
        val sakId = 1L
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                status = BehandlingStatus.FATTET_VEDTAK,
                boddEllerArbeidetUtlandet =
                    BoddEllerArbeidetUtlandet(
                        boddEllerArbeidetUtlandet = true,
                        skalSendeKravpakke = true,
                        begrunnelse = "beg",
                        kilde = Grunnlagsopplysning.Saksbehandler.create("navIdent"),
                    ),
            )
        val behandlingId = behandling.id
        val vedtakId = 1L

        val behandlingdao =
            mockk<BehandlingDao> {
                every { lagreStatus(any(), any(), any()) } just runs
            }

        val vedtakHendelse = VedtakHendelse(vedtakId, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto = VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.AVSLAG)

        val behandlingService =
            mockk<BehandlingService> {
                every { registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT) } just runs
                every { hentBehandling(behandlingId) } returns behandling
            }
        val grlService = mockk<GrunnlagsendringshendelseService>()

        val generellBehandlingUtland =
            GenerellBehandling.opprettUtland(
                sakId,
                behandlingId,
            )
        val generellBehandlingService =
            mockk<GenerellBehandlingService> {
                every { opprettBehandling(any(), any()) } returns generellBehandlingUtland
            }

        val sut =
            BehandlingStatusServiceImpl(
                behandlingdao,
                behandlingService,
                grlService,
                generellBehandlingService,
            )

        inTransaction {
            sut.settAttestertVedtak(behandling, vedtakEndringDto)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.AVSLAG, any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
            generellBehandlingService.opprettBehandling(any(), any())
        }
        confirmVerified(behandlingdao, behandlingService, grlService, generellBehandlingService)
    }

    @Test
    fun `attestert vedtak som ikke er avslag skal ikke ha kravpakke(utland)`() {
        val sakId = 1L
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                status = BehandlingStatus.FATTET_VEDTAK,
                boddEllerArbeidetUtlandet =
                    BoddEllerArbeidetUtlandet(
                        boddEllerArbeidetUtlandet = true,
                        skalSendeKravpakke = true,
                        begrunnelse = "beg",
                        kilde = Grunnlagsopplysning.Saksbehandler.create("navIdent"),
                    ),
            )
        val behandlingId = behandling.id
        val vedtakId = 1L

        val behandlingdao =
            mockk<BehandlingDao> {
                every { lagreStatus(any(), any(), any()) } just runs
            }

        val vedtakHendelse = VedtakHendelse(vedtakId, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto = VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.INNVILGELSE)

        val behandlingService =
            mockk<BehandlingService> {
                every { registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT) } just runs
                every { hentBehandling(behandlingId) } returns behandling
            }
        val grlService = mockk<GrunnlagsendringshendelseService>()
        val generellBehandlingService = mockk<GenerellBehandlingService>()

        val sut =
            BehandlingStatusServiceImpl(
                behandlingdao,
                behandlingService,
                grlService,
                generellBehandlingService,
            )

        inTransaction {
            sut.settAttestertVedtak(behandling, vedtakEndringDto)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.ATTESTERT, any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
        }
        confirmVerified(behandlingdao, behandlingService, grlService, generellBehandlingService)
    }
}
