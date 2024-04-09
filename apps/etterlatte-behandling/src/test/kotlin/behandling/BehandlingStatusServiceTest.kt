package no.nav.etterlatte.behandling

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingStatusServiceTest {
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>(relaxUnitFun = true)
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val generellBehandlingService = mockk<GenerellBehandlingService>()
    private val behandlingdao = mockk<BehandlingDao>(relaxUnitFun = true)

    private val brukerTokenInfo = BrukerTokenInfo.of(UUID.randomUUID().toString(), "Z123456", null, null, null)

    private val sut =
        BehandlingStatusServiceImpl(
            behandlingdao,
            behandlingService,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
        )

    @BeforeEach
    fun before() {
        nyKontekstMedBruker(user)
    }

    @AfterEach
    fun after() {
        confirmVerified(
            behandlingdao,
            behandlingService,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
        )
        clearAllMocks()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VILKAARSVURDERT",
            "BEREGNET",
            "AVKORTET",
            "RETURNERT",
        ],
    )
    fun `Fattet vedtak til attestering`(status: BehandlingStatus) {
        val sakId = 1L
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                status = status,
                gyldighetsproeving =
                    GyldighetsResultat(
                        resultat = VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                        vurderinger = emptyList(),
                        vurdertDato = LocalDateTime.now(),
                    ),
                virkningstidspunkt =
                    Virkningstidspunkt(
                        dato = YearMonth.now(),
                        kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                        begrunnelse = "Ingen",
                    ),
                kommerBarnetTilgode =
                    KommerBarnetTilgode(
                        JaNei.JA,
                        "",
                        Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                    ),
            )
        val behandlingId = behandling.id
        val vedtakHendelse = VedtakHendelse(1L, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto =
            VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.AVSLAG)

        every { oppgaveService.tilAttestering(any(), any(), any()) } returns mockk()

        inTransaction {
            sut.settFattetVedtak(behandling, vedtakEndringDto, brukerTokenInfo)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.FATTET_VEDTAK, any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.FATTET)
            oppgaveService.tilAttestering(
                behandlingId.toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                any<String>(),
                null,
            )
        }
    }

    @Test
    fun `iverksettNasjonal behandling`() {
        val sakId = 1L
        val behandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val behandlingId = behandling.id
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), "sbl")

        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        inTransaction {
            sut.settIverksattVedtak(behandlingId, iverksettVedtak)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.IVERKSATT, any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
        }
    }

    @Test
    fun `underkjent behandling`() {
        val sakId = 1L
        val behandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.FATTET_VEDTAK)
        val behandlingId = behandling.id

        val vedtakHendelse = VedtakHendelse(1L, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto =
            VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.AVSLAG)

        every { oppgaveService.tilUnderkjent(any(), any(), any()) } returns mockk()

        inTransaction {
            sut.settReturnertVedtak(behandling, vedtakEndringDto)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.RETURNERT, any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.UNDERKJENT)
            oppgaveService.tilUnderkjent(behandlingId.toString(), OppgaveType.FOERSTEGANGSBEHANDLING, any())
        }
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

        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        val generellBehandlingUtland =
            GenerellBehandling.opprettUtland(
                sakId,
                behandlingId,
            )

        every { generellBehandlingService.opprettBehandling(any(), any()) } returns generellBehandlingUtland

        inTransaction {
            sut.settIverksattVedtak(behandlingId, iverksattVedtak)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.IVERKSATT, any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksattVedtak, HendelseType.IVERKSATT)
            generellBehandlingService.opprettBehandling(any(), any())
        }
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

        val vedtakHendelse = VedtakHendelse(vedtakId, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto =
            VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.AVSLAG)

        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        val generellBehandlingUtland =
            GenerellBehandling.opprettUtland(
                sakId,
                behandlingId,
            )
        every { generellBehandlingService.opprettBehandling(any(), any()) } returns generellBehandlingUtland
        every { oppgaveService.ferdigStillOppgaveUnderBehandling(any(), any(), any(), any()) } returns mockk()

        inTransaction {
            sut.settAttestertVedtak(behandling, vedtakEndringDto, brukerTokenInfo)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.AVSLAG, any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
            generellBehandlingService.opprettBehandling(any(), any())
            oppgaveService.ferdigStillOppgaveUnderBehandling(
                behandlingId.toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                any(),
                any(),
            )
        }
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

        val vedtakHendelse = VedtakHendelse(vedtakId, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto =
            VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.INNVILGELSE)

        every { oppgaveService.ferdigStillOppgaveUnderBehandling(any(), any(), any(), any()) } returns mockk()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        inTransaction {
            sut.settAttestertVedtak(behandling, vedtakEndringDto, brukerTokenInfo)
        }

        verify {
            behandlingdao.lagreStatus(behandlingId, BehandlingStatus.ATTESTERT, any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
            oppgaveService.ferdigStillOppgaveUnderBehandling(
                behandlingId.toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                brukerTokenInfo,
                any<String>(),
            )
        }
    }
}
