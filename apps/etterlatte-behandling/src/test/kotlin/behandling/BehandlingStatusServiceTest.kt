package no.nav.etterlatte.behandling

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.domain.AutomatiskRevurdering
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingStatusServiceTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>(relaxUnitFun = true)
    private val behandlingInfoDao = mockk<BehandlingInfoDao>(relaxUnitFun = true)
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val generellBehandlingService = mockk<GenerellBehandlingService>()
    private val behandlingDao = mockk<BehandlingDao>(relaxUnitFun = true)
    private val aktivitetspliktService = mockk<AktivitetspliktService>(relaxUnitFun = true)
    private val saksbehandlerService: SaksbehandlerService = mockk()
    private val etteroppgjoerService: EtteroppgjoerService = mockk()
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService = mockk()
    private val grunnlagService: GrunnlagService = mockk()

    private val brukerTokenInfo = simpleSaksbehandler("Z123456")

    private val sut =
        BehandlingStatusServiceImpl(
            behandlingDao,
            behandlingService,
            behandlingInfoDao,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
            aktivitetspliktService,
            saksbehandlerService,
            etteroppgjoerService,
            etteroppgjoerForbehandlingService,
            grunnlagService,
        )

    @BeforeEach
    fun before() {
        val user = mockk<SaksbehandlerMedEnheterOgRoller>().also { every { it.name() } returns this::class.java.simpleName }
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Roald Amundsen"
        nyKontekstMedBruker(user)
    }

    @AfterEach
    fun after() {
        confirmVerified(
            saksbehandlerService,
            behandlingDao,
            behandlingService,
            oppgaveService,
            behandlingInfoDao,
            grunnlagsendringshendelseService,
            generellBehandlingService,
        )
        clearAllMocks(currentThreadOnly = true)
    }

    @Test
    fun `Revurdering av type inntektsendring som er automatisk skal flyttes til manuell prosess hvis underkjent attestering`() {
        val revurdering =
            AutomatiskRevurdering(
                id = UUID.randomUUID(),
                sak = mockk(),
                behandlingOpprettet = LocalDateTime.now(),
                status = BehandlingStatus.FATTET_VEDTAK,
                kommerBarnetTilgode = null,
                virkningstidspunkt = null,
                boddEllerArbeidetUtlandet = null,
                soeknadMottattDato = null,
                revurderingsaarsak = Revurderingaarsak.INNTEKTSENDRING,
                revurderingInfo = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                begrunnelse = "",
                relatertBehandlingId = null,
                opphoerFraOgMed = null,
                tidligereFamiliepleier = null,
                sendeBrev = true,
                sistEndret = LocalDateTime.now(),
                utlandstilknytning = null,
                opprinnelse = BehandlingOpprinnelse.UKJENT,
            )
        val vedtaksendring =
            VedtakEndringDTO(
                sakIdOgReferanse = SakIdOgReferanse(SakId(123L), ""),
                vedtakHendelse = VedtakHendelse(123L, Tidspunkt.now(), null, null, null),
                vedtakType = VedtakType.ENDRING,
            )

        every { behandlingDao.lagreStatus(any()) } just runs
        every { behandlingService.registrerVedtakHendelse(any(), any(), any()) } just runs
        every { oppgaveService.tilUnderkjent(any(), any(), any()) } returns mockk()
        every { behandlingService.endreProsesstype(any(), any()) } just runs
        sut.settReturnertVedtak(revurdering, vedtaksendring, brukerTokenInfo)

        verify { behandlingService.endreProsesstype(revurdering.id, Prosesstype.MANUELL) }
        verify { behandlingDao.lagreStatus(any()) }
        verify { behandlingService.registrerVedtakHendelse(any(), any(), any()) }
        verify { oppgaveService.tilUnderkjent(any(), any(), any()) }
        verify { saksbehandlerService.hentNavnForIdent(any()) }
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
        val sakId = sakId1
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
            behandlingDao.lagreStatus(any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.FATTET)
            oppgaveService.tilAttestering(
                behandlingId.toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                any<String>(),
            )
        }
    }

    @Test
    fun `iverksettNasjonal behandling`() {
        val sakId = sakId1
        val behandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.ATTESTERT)
        val behandlingId = behandling.id
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), "sbl")

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingInfoDao.hentBrevutfall(behandlingId) } returns brevutfallDto(behandlingId)

        inTransaction {
            runBlocking {
                sut.settIverksattVedtak(behandlingId, iverksettVedtak)
            }
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
            behandlingInfoDao.hentBrevutfall(behandlingId)
        }
    }

    @Test
    fun `iverksett vedtak skal opprette etteroppgjør`() {
        val sakId = sakId1
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.of(ETTEROPPGJOER_AAR, Month.MARCH)),
            )
        val behandlingId = behandling.id
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), "sbl")

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingInfoDao.hentBrevutfall(behandlingId) } returns brevutfallDto(behandlingId)
        coEvery {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                behandling = behandling,
                inntektsaar = any(),
            )
        } returns mockk<Etteroppgjoer>()
        every { grunnlagService.hentPersonopplysninger(any(), any()) } returns
            mockk {
                every { avdoede } returns
                    listOf(
                        mockk {
                            every { opplysning } returns
                                mockk {
                                    every { doedsdato } returns LocalDate.now()
                                }
                        },
                    )
            }

        inTransaction {
            runBlocking {
                sut.settIverksattVedtak(behandlingId, iverksettVedtak)
            }
        }

        coVerify {
            behandlingDao.lagreStatus(any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
            behandlingInfoDao.hentBrevutfall(behandlingId)
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                behandling = behandling,
                inntektsaar = any(),
            )
        }
    }

    @Test
    fun `opprettEtteroppgjoerHvisTilbakevirkendeFoerstegangsbehandling oppretter etteroppgjoer for flere aar`() {
        val sakId = sakId1
        val virkAar = Year.now().value - 2 // Sett virkningstidspunkt til 2 år tilbake i tid
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.of(virkAar, Month.MARCH)),
            )

        coEvery {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                behandling = behandling,
                inntektsaar = any(),
            )
        } returns mockk<Etteroppgjoer>()

        runBlocking {
            sut.opprettEtteroppgjoerHvisTilbakevirkendeFoerstegangsbehandling(behandling, virkAar)
        }

        // F.eks inneværende år er 2026, så etteroppgjør skal opprettes for 2024 og 2025
        coVerify(exactly = 1) {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(behandling, Year.now().value - 2)
        }
        coVerify(exactly = 1) {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(behandling, Year.now().value - 1)
        }
    }

    @Test
    fun `opprettEtteroppgjoerHvisTilbakevirkendeFoerstegangsbehandling oppretter ikke etteroppgjoer hvis virkAar er innevaerende aar`() {
        val sakId = sakId1
        val virkAar = Year.now().value
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.of(virkAar, Month.MARCH)),
            )

        runBlocking {
            sut.opprettEtteroppgjoerHvisTilbakevirkendeFoerstegangsbehandling(behandling, virkAar)
        }

        // Ingen etteroppgjør skal opprettes når virkningstidspunkt er inneværende år
        coVerify(exactly = 0) {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(any(), any())
        }
    }

    @Test
    fun `underkjent behandling`() {
        val sakId = sakId1
        val behandling = foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.FATTET_VEDTAK)
        val behandlingId = behandling.id

        val vedtakHendelse = VedtakHendelse(1L, Tidspunkt.now(), "saksbehandler")
        val vedtakEndringDto =
            VedtakEndringDTO(SakIdOgReferanse(sakId, behandlingId.toString()), vedtakHendelse, VedtakType.AVSLAG)

        every { oppgaveService.tilUnderkjent(any(), any(), any()) } returns mockk()

        inTransaction {
            sut.settReturnertVedtak(behandling, vedtakEndringDto, brukerTokenInfo)
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.UNDERKJENT)
            oppgaveService.tilUnderkjent(behandlingId.toString(), OppgaveType.FOERSTEGANGSBEHANDLING, any())
        }
        verify(exactly = 1) { saksbehandlerService.hentNavnForIdent(any()) }
    }

    @Test
    fun `iverksett utlandstilsnitt behandling`() {
        val sakId = sakId1
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
        every { behandlingInfoDao.hentBrevutfall(behandlingId) } returns brevutfallDto(behandlingId)

        val generellBehandlingUtland =
            GenerellBehandling.opprettUtland(
                sakId,
                behandlingId,
            )

        every { generellBehandlingService.opprettBehandling(any(), any()) } returns generellBehandlingUtland

        inTransaction {
            runBlocking {
                sut.settIverksattVedtak(behandlingId, iverksattVedtak)
            }
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksattVedtak, HendelseType.IVERKSATT)
            generellBehandlingService.opprettBehandling(any(), any())
            behandlingInfoDao.hentBrevutfall(behandlingId)
        }
    }

    @Test
    fun `utlandstilsnitt avslag behandling ikke oppfylt vilkårsvurdering`() {
        val sakId = sakId1
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
        every { oppgaveService.ferdigstillOppgaveUnderBehandling(any(), any(), any(), any()) } returns mockk()

        inTransaction {
            sut.settAttestertVedtak(behandling, vedtakEndringDto, brukerTokenInfo)
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
            generellBehandlingService.opprettBehandling(any(), any())
            oppgaveService.ferdigstillOppgaveUnderBehandling(
                behandlingId.toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                any(),
                any(),
            )
        }
        verify(exactly = 1) { saksbehandlerService.hentNavnForIdent(any()) }
    }

    @Test
    fun `attestert vedtak som ikke er avslag skal ikke ha kravpakke(utland)`() {
        val sakId = sakId1
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

        every { oppgaveService.ferdigstillOppgaveUnderBehandling(any(), any(), any(), any()) } returns mockk()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        inTransaction {
            sut.settAttestertVedtak(behandling, vedtakEndringDto, brukerTokenInfo)
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.ATTESTERT)
            oppgaveService.ferdigstillOppgaveUnderBehandling(
                behandlingId.toString(),
                OppgaveType.FOERSTEGANGSBEHANDLING,
                brukerTokenInfo,
                any<String>(),
            )
            saksbehandlerService.hentNavnForIdent(any())
        }
    }

    @ParameterizedTest()
    @EnumSource(FeilutbetalingValg::class, names = ["JA_VARSEL", "JA_INGEN_TK"], mode = EnumSource.Mode.INCLUDE)
    fun `skal opprette tilbakekrevingsoppgave naar behandling med feilutbetaling blir iverksatt`(feilutbetalingValg: FeilutbetalingValg) {
        val sakId = sakId1
        val behandling =
            revurdering(
                sakId = sakId,
                revurderingAarsak = Revurderingaarsak.ANNEN,
                status = BehandlingStatus.ATTESTERT,
            )
        val behandlingId = behandling.id
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), "sbl")

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingInfoDao.hentBrevutfall(behandlingId) } returns brevutfallDto(behandlingId, feilutbetalingValg)
        every { oppgaveService.hentOppgaverForSak(sakId, any()) } returns
            listOf(
                oppgave(UUID.randomUUID(), sakId, Status.FERDIGSTILT),
                oppgave(UUID.randomUUID(), sakId, Status.FEILREGISTRERT),
                oppgave(UUID.randomUUID(), sakId, Status.AVBRUTT),
            )
        every { oppgaveService.opprettOppgave(any(), sakId, any(), any(), any()) } returns mockk()
        every { grunnlagsendringshendelseService.settHendelseTilHistorisk(behandlingId) } just runs

        inTransaction {
            runBlocking {
                sut.settIverksattVedtak(behandlingId, iverksettVedtak)
            }
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
            behandlingInfoDao.hentBrevutfall(behandlingId)
            oppgaveService.hentOppgaverForSak(sakId, OppgaveType.TILBAKEKREVING)
            oppgaveService.opprettOppgave(
                referanse = sakId.toString(),
                sakId = sakId,
                kilde = OppgaveKilde.TILBAKEKREVING,
                type = OppgaveType.TILBAKEKREVING,
                merknad = "Venter på kravgrunnlag",
            )
            grunnlagsendringshendelseService.settHendelseTilHistorisk(behandlingId)
        }
    }

    @ParameterizedTest()
    @EnumSource(FeilutbetalingValg::class, names = ["JA_VARSEL", "JA_INGEN_TK"], mode = EnumSource.Mode.INCLUDE)
    fun `skal sette eksisterende tilbakekrevingsoppgave paa vent naar behandling med feilutbetaling blir iverksatt`(
        feilutbetalingValg: FeilutbetalingValg,
    ) {
        val oppgaveId = UUID.randomUUID()
        val sakId = sakId1
        val behandling =
            revurdering(
                sakId = sakId,
                revurderingAarsak = Revurderingaarsak.ANNEN,
                status = BehandlingStatus.ATTESTERT,
            )
        val behandlingId = behandling.id
        val iverksettVedtak = VedtakHendelse(1L, Tidspunkt.now(), "sbl")

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingInfoDao.hentBrevutfall(behandlingId) } returns brevutfallDto(behandlingId, feilutbetalingValg)
        every { oppgaveService.hentOppgaverForSak(sakId, OppgaveType.TILBAKEKREVING) } returns listOf(oppgave(oppgaveId, sakId))
        every { oppgaveService.endrePaaVent(any(), any(), any(), any()) } returns oppgave(oppgaveId, sakId, Status.PAA_VENT)
        every { grunnlagsendringshendelseService.settHendelseTilHistorisk(behandlingId) } just runs

        inTransaction {
            runBlocking {
                sut.settIverksattVedtak(behandlingId, iverksettVedtak)
            }
        }

        verify {
            behandlingDao.lagreStatus(any())
            behandlingService.hentBehandling(behandlingId)
            behandlingService.registrerVedtakHendelse(behandlingId, iverksettVedtak, HendelseType.IVERKSATT)
            behandlingInfoDao.hentBrevutfall(behandlingId)
            oppgaveService.hentOppgaverForSak(sakId, OppgaveType.TILBAKEKREVING)
            oppgaveService.endrePaaVent(oppgaveId, true, "Venter på oppdatert kravgrunnlag", PaaVentAarsak.KRAVGRUNNLAG_SPERRET)
            grunnlagsendringshendelseService.settHendelseTilHistorisk(behandlingId)
        }
    }

    private fun oppgave(
        oppgaveId: UUID = UUID.randomUUID(),
        sakId: SakId,
        status: Status = Status.UNDER_BEHANDLING,
    ) = OppgaveIntern(
        id = oppgaveId,
        status = status,
        enhet = Enheter.defaultEnhet.enhetNr,
        sakId = sakId,
        kilde = OppgaveKilde.TILBAKEKREVING,
        type = OppgaveType.TILBAKEKREVING,
        saksbehandler = null,
        referanse = sakId.toString(),
        gruppeId = null,
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = SakType.BARNEPENSJON,
        fnr = null,
        frist = Tidspunkt.now(),
    )

    private fun brevutfallDto(
        behandlingId: UUID,
        feilutbetalingValg: FeilutbetalingValg = FeilutbetalingValg.NEI,
    ) = BrevutfallDto(
        behandlingId = behandlingId,
        aldersgruppe = null,
        feilutbetaling = Feilutbetaling(feilutbetalingValg, "kommentar"),
        kilde = Grunnlagsopplysning.Saksbehandler("123456", Tidspunkt.now()),
        frivilligSkattetrekk = true,
    )
}
