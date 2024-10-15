package no.nav.etterlatte.behandling.aktivitetsplikt

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JobbType
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktVarigUnntakDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AktivitetspliktServiceTest {
    private val aktivitetspliktDao: AktivitetspliktDao = mockk()
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao = mockk()
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val revurderingService: RevurderingService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val statistikkProduer: BehandlingHendelserKafkaProducer = mockk()
    private val featureToggleService: FeatureToggleService = DummyFeatureToggleService()
    private val service =
        AktivitetspliktService(
            aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao,
            behandlingService,
            grunnlagKlient,
            revurderingService,
            statistikkProduer,
            oppgaveService,
        )
    private val user =
        mockk<SaksbehandlerMedEnheterOgRoller>().also { every { it.name() } returns this::class.java.simpleName }
    private val brukerTokenInfo =
        mockk<BrukerTokenInfo> {
            every { ident() } returns "Z999999"
        }

    @BeforeEach
    fun setup() {
        nyKontekstMedBruker(user)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class HentAktiviteter {
        @Test
        fun `Skal returnere liste med aktiviteter`() {
            val behandlingId = UUID.randomUUID()
            val aktivitet = mockk<AktivitetspliktAktivitet>()
            every { aktivitetspliktDao.hentAktiviteterForBehandling(behandlingId) } returns listOf(aktivitet)

            val result = service.hentAktiviteter(behandlingId)

            result shouldBe listOf(aktivitet)
        }
    }

    @Nested
    inner class OpprettEllerOppdaterAktivitet {
        @Test
        fun `Skal opprette en aktivitet`() {
            every { aktivitetspliktDao.opprettAktivitet(behandling.id, aktivitet, any()) } returns 1

            service.upsertAktivitet(aktivitet, brukerTokenInfo, behandling.id)

            coVerify { aktivitetspliktDao.opprettAktivitet(behandling.id, aktivitet, any()) }
            coVerify(exactly = 0) { aktivitetspliktDao.oppdaterAktivitet(any(), any(), any()) }
        }

        @Test
        fun `Skal oppdatere en aktivitet`() {
            val aktivitet = aktivitet.copy(id = UUID.randomUUID())
            every { aktivitetspliktDao.oppdaterAktivitet(behandling.id, aktivitet, any()) } returns 1

            service.upsertAktivitet(aktivitet, brukerTokenInfo, behandling.id)

            coVerify { aktivitetspliktDao.oppdaterAktivitet(behandling.id, aktivitet, any()) }
            coVerify(exactly = 0) { aktivitetspliktDao.opprettAktivitet(any(), any(), any()) }
        }

        @Test
        fun `Skal kaste feil hvis sakId ikke stemmer overens med behandling`() {
            val aktivitet = aktivitet.copy(sakId = sakId2)

            assertThrows<SakidTilhoererIkkeBehandlingException> {
                service.upsertAktivitet(aktivitet, brukerTokenInfo, behandling.id)
            }
        }

        @Test
        fun `Skal kaste feil hvis tom er foer fom`() {
            val aktivitet = aktivitet.copy(tom = LocalDate.now().minusYears(1))

            assertThrows<TomErFoerFomException> {
                service.upsertAktivitet(aktivitet, brukerTokenInfo, behandling.id)
            }
        }

        @Test
        fun `Skal kaste feil hvis behandlingen ikke kan endres`() {
            val behandling =
                behandling.apply {
                    every { status } returns BehandlingStatus.ATTESTERT
                }
            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            assertThrows<BehandlingKanIkkeEndres> {
                service.upsertAktivitet(aktivitet, brukerTokenInfo, behandling.id)
            }
        }
    }

    @Nested
    inner class SlettAktivitet {
        private val aktivitetId = UUID.randomUUID()

        @Test
        fun `Skal slette en aktivitet`() {
            every { aktivitetspliktDao.slettAktivitet(aktivitetId, behandling.id) } returns 1
            every { behandlingService.hentBehandling(behandling.id) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                }

            service.slettAktivitet(aktivitetId, brukerTokenInfo, behandling.id)

            coVerify { aktivitetspliktDao.slettAktivitet(aktivitetId, behandling.id) }
        }

        @Test
        fun `Skal kaste feil hvis behandling ikke kan endres`() {
            every { behandlingService.hentBehandling(behandling.id) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.FATTET_VEDTAK
                }

            assertThrows<BehandlingKanIkkeEndres> {
                service.slettAktivitet(aktivitetId, brukerTokenInfo, behandling.id)
            }
        }
    }

    @Nested
    inner class OpprettOgHentAktivitetsgrad {
        private val oppgaveId = UUID.randomUUID()
        private val sakId = sakId1

        @Test
        fun `Skal opprette en ny aktivitetsgrad`() {
            val aktivitetsgrad =
                LagreAktivitetspliktAktivitetsgrad(
                    aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                    beskrivelse = "Beskrivelse",
                )
            every {
                aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(
                    aktivitetsgrad,
                    sakId,
                    any(),
                    oppgaveId,
                )
            } returns 1
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns emptyList()
            every { oppgaveService.hentOppgave(oppgaveId) } returns
                OppgaveIntern(
                    id = UUID.randomUUID(),
                    status = Status.NY,
                    enhet = Enheter.defaultEnhet.enhetNr,
                    sakId = sakId,
                    kilde = OppgaveKilde.BEHANDLING,
                    type = OppgaveType.AKTIVITETSPLIKT,
                    referanse = UUID.randomUUID().toString(),
                    opprettet = Tidspunkt.now(),
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    frist = null,
                )
            service.opprettAktivitetsgradForOppgave(aktivitetsgrad, oppgaveId, sakId, brukerTokenInfo)

            verify { aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(aktivitetsgrad, sakId, any(), oppgaveId) }
            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) }
        }

        @Test
        fun `Skal ikke opprette en ny aktivitetsgrad hvis det finnes fra foer`() {
            val aktivitetsgrad =
                LagreAktivitetspliktAktivitetsgrad(
                    aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                    beskrivelse = "Beskrivelse",
                )
            every {
                aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(
                    aktivitetsgrad,
                    sakId,
                    any(),
                    oppgaveId,
                )
            } returns 1
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns listOf(mockk())

            assertThrows<IllegalArgumentException> {
                service.opprettAktivitetsgradForOppgave(aktivitetsgrad, oppgaveId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal hente en vurdering med aktivitetsgrad basert paa oppgaveId`() {
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns listOf(mockk())
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns emptyList()

            val vurdering = service.hentVurderingForOppgave(oppgaveId)

            vurdering shouldNotBe null
            vurdering?.aktivitet?.isEmpty() shouldNotBe true
            vurdering?.unntak shouldBe emptyList()

            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) }
        }
    }

    @Nested
    inner class OpprettOgHentUnntakForOppgave {
        private val oppgaveId = UUID.randomUUID()
        private val sakId = sakId1

        @Test
        fun `Skal opprette en nytt unntak`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = null,
                    tom = LocalDate.now().plusMonths(6),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), oppgaveId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns emptyList()
            every { oppgaveService.hentOppgave(oppgaveId) } returns
                OppgaveIntern(
                    id = UUID.randomUUID(),
                    status = Status.NY,
                    enhet = Enheter.defaultEnhet.enhetNr,
                    sakId = sakId,
                    kilde = OppgaveKilde.BEHANDLING,
                    type = OppgaveType.AKTIVITETSPLIKT,
                    referanse = UUID.randomUUID().toString(),
                    opprettet = Tidspunkt.now(),
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    frist = null,
                )
            service.opprettUnntakForOpppgave(unntak, oppgaveId, sakId, brukerTokenInfo)

            verify { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), oppgaveId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) }
        }

        @Test
        fun `Skal ikke opprette en et nytt unntak hvis det finnes fra foer`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(6),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), oppgaveId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns listOf(mockk())

            assertThrows<IllegalArgumentException> {
                service.opprettUnntakForOpppgave(unntak, oppgaveId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal kaste feil hvis tom er foer fom`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = LocalDate.now().plusMonths(6),
                    tom = LocalDate.now().plusMonths(0),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), oppgaveId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns emptyList()

            assertThrows<TomErFoerFomException> {
                service.opprettUnntakForOpppgave(unntak, oppgaveId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal hente en vurdering med unntak basert paa oppgaveId`() {
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns listOf(mockk())

            val vurdering = service.hentVurderingForOppgave(oppgaveId)

            vurdering shouldNotBe null
            vurdering?.aktivitet shouldBe emptyList()
            vurdering?.unntak?.isEmpty() shouldBe false

            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) }
        }
    }

    @Nested
    inner class OpprettOgHentUnntakForBehandling {
        private val behandlingId = UUID.randomUUID()
        private val sakId = sakId1

        @Test
        fun `Skal opprette en nytt unntak`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = null,
                    tom = LocalDate.now().plusMonths(6),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns emptyList()
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns emptyList()
            every { behandlingService.hentBehandling(behandlingId) } returns behandling

            service.upsertUnntakForBehandling(unntak, behandlingId, sakId, brukerTokenInfo)

            verify { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
        }

        @Test
        fun `Skal slette aktivitetsgrad hvis man oppretter et nytt unntak`() {
            val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
            val aktivitetsgradId = UUID.randomUUID()

            val aktivitetsgrad =
                LagreAktivitetspliktAktivitetsgrad(
                    aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                    beskrivelse = "Beskrivelse",
                )

            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = null,
                    tom = LocalDate.now().plusMonths(6),
                )

            every { behandlingService.hentBehandling(behandlingId) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                }
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns emptyList()
            every {
                aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(
                    aktivitetsgrad,
                    sakId,
                    any(),
                    behandlingId = behandlingId,
                )
            } returns 1
            every { aktivitetspliktAktivitetsgradDao.slettAktivitetsgrad(aktivitetsgradId, behandlingId) } returns 1
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns
                listOf(
                    AktivitetspliktAktivitetsgrad(
                        id = aktivitetsgradId,
                        sakId = sakId,
                        behandlingId = behandlingId,
                        oppgaveId = null,
                        aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                        fom = LocalDate.now(),
                        tom = null,
                        opprettet = kilde,
                        endret = kilde,
                        beskrivelse = "Beskrivelse",
                    ),
                )

            aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(
                aktivitetsgrad,
                sakId,
                kilde,
                behandlingId = behandlingId,
            )
            service.upsertUnntakForBehandling(unntak, behandlingId, sakId, brukerTokenInfo)

            verify { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
            verify { aktivitetspliktAktivitetsgradDao.slettAktivitetsgrad(aktivitetsgradId, behandlingId) }
        }

        @Test
        fun `Skal ikke opprette et nytt unntak hvis det finnes fra foer`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(6),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns listOf(mockk())
            every { behandlingService.hentBehandling(behandlingId) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                }

            assertThrows<IllegalArgumentException> {
                service.upsertUnntakForBehandling(unntak, behandlingId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal kaste feil hvis tom er foer fom`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = LocalDate.now().plusMonths(6),
                    tom = LocalDate.now().plusMonths(0),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns emptyList()

            assertThrows<TomErFoerFomException> {
                service.opprettUnntakForOpppgave(unntak, behandlingId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal hente en vurdering med unntak basert paa behandlingId`() {
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns listOf(mockk())

            val vurdering = service.hentVurderingForBehandling(behandlingId)

            vurdering shouldNotBe null
            vurdering?.aktivitet shouldBe emptyList()
            vurdering?.unntak?.isEmpty() shouldBe false

            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
        }
    }

    @Nested
    inner class OppdaterUnntakForBehandling {
        private val behandlingId = UUID.randomUUID()
        private val sakId = sakId1

        @Test
        fun `Skal oppdatere unntak hvis id finnes`() {
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = null,
                    tom = LocalDate.now().plusMonths(6),
                )

            val unntakMedId =
                LagreAktivitetspliktUnntak(
                    id = UUID.randomUUID(),
                    unntak = AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT,
                    beskrivelse = "Beskrivelse er oppdatert",
                    fom = null,
                    tom = LocalDate.now().plusMonths(6),
                )
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.oppdaterUnntak(unntakMedId, any(), behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns emptyList()
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns emptyList()
            every { behandlingService.hentBehandling(behandlingId) } returns behandling

            service.upsertUnntakForBehandling(unntak, behandlingId, sakId, brukerTokenInfo)
            service.upsertUnntakForBehandling(unntakMedId, behandlingId, sakId, brukerTokenInfo)

            verify { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) }
            verify { aktivitetspliktUnntakDao.oppdaterUnntak(unntakMedId, any(), behandlingId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
        }
    }

    @Nested
    inner class OppfyllerAktivitetsplikt {
        @Test
        fun `Skal returnere true hvis aktivitetsplikt er oppfylt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                listOf(
                    mockk {
                        every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
                        every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                        every { sakId } returns aktivitet.sakId
                        every { fom } returns LocalDate.of(2024, Month.APRIL, 20)
                        every { behandlingId } returns UUID.randomUUID()
                        every { oppgaveId } returns null
                    },
                )
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns emptyList()

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe true
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt og det ikke finnes unntak`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                listOf(
                    mockk {
                        every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                        every { fom } returns aktivitet.fom.minusMonths(1)
                        every { sakId } returns aktivitet.sakId
                        every { behandlingId } returns UUID.randomUUID()
                        every { oppgaveId } returns null
                    },
                )
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns emptyList()

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe false
        }

        @Test
        fun `Skal returnere true hvis aktivitetsplikt ikke er oppfylt, hvis det finnes unntak for perioden`() {
            val foerst = Grunnlagsopplysning.Saksbehandler.create("Z123455")
            val sist = Grunnlagsopplysning.Saksbehandler.create("Z123455")
            val behId = UUID.randomUUID()
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                listOf(
                    mockk {
                        every { fom } returns null
                        every { tom } returns null
                        every { opprettet } returns foerst
                        every { sakId } returns aktivitet.sakId
                        every { behandlingId } returns behId
                        every { oppgaveId } returns null
                    },
                )
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                listOf(
                    mockk {
                        every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                        every { fom } returns aktivitet.fom.minusMonths(1)
                        every { opprettet } returns sist
                        every { sakId } returns aktivitet.sakId
                        every { behandlingId } returns behId
                        every { oppgaveId } returns null
                    },
                )

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe true
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt og unntaket er utgaatt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                listOf(
                    mockk {
                        every { fom } returns null
                        every { tom } returns LocalDate.now().minusYears(1)
                        every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                        every { sakId } returns aktivitet.sakId
                        every { behandlingId } returns UUID.randomUUID()
                        every { oppgaveId } returns null
                    },
                )

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe false
        }
    }

    @Nested
    inner class OpprettRevurderingHvisKravIkkeOppfylt {
        private val sakId = sakId1
        private val forrigeBehandling: Behandling =
            mockk {
                every { id } returns UUID.randomUUID()
                every { status } returns BehandlingStatus.IVERKSATT
                every { utlandstilknytning } returns null
                every { boddEllerArbeidetUtlandet } returns null
                every { opphoerFraOgMed } returns null
            }
        private val frist = Tidspunkt.now()
        private val request =
            OpprettRevurderingForAktivitetspliktDto(
                sakId = sakId,
                frist = frist,
                behandlingsmaaned = YearMonth.now(),
                jobbType = JobbType.OMS_DOED_6MND,
            )
        private val persongalleri: Persongalleri = mockk()
        private val persongalleriOpplysning =
            mockk<Grunnlagsopplysning<Persongalleri>> {
                every { opplysning } returns persongalleri
            }

        @Test
        fun `Skal opprette revurdering hvis kravene for aktivitetsplikt ikke er oppfylt`() {
            val revurdering =
                mockk<Revurdering> {
                    every { id } returns UUID.randomUUID()
                }
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns emptyList()
            every { behandlingService.hentSisteIverksatte(sakId) } returns forrigeBehandling
            every { behandlingService.hentBehandlingerForSak(sakId) } returns listOf(forrigeBehandling)
            coEvery { grunnlagKlient.hentPersongalleri(forrigeBehandling.id, any()) } returns persongalleriOpplysning
            every {
                revurderingService.opprettRevurdering(
                    sakId = sakId,
                    persongalleri = persongalleriOpplysning.opplysning,
                    forrigeBehandling = forrigeBehandling.id,
                    prosessType = Prosesstype.MANUELL,
                    kilde = Vedtaksloesning.GJENNY,
                    revurderingAarsak = Revurderingaarsak.AKTIVITETSPLIKT,
                    virkningstidspunkt =
                        request.behandlingsmaaned
                            .atDay(1)
                            .plusMonths(1)
                            .tilVirkningstidspunkt("Aktivitetsplikt"),
                    utlandstilknytning = null,
                    boddEllerArbeidetUtlandet = null,
                    begrunnelse = request.jobbType.beskrivelse,
                    saksbehandlerIdent = Fagsaksystem.EY.navn,
                    frist = frist,
                    opphoerFraOgMed = null,
                    mottattDato = null,
                )
            } returns
                mockk { every { oppdater() } returns revurdering }
            every { oppgaveService.fjernSaksbehandler(any()) } just runs
            every { oppgaveService.hentOppgaverForReferanse(any()) } returns
                listOf(
                    mockk {
                        every { type } returns OppgaveType.REVURDERING
                        every { id } returns UUID.randomUUID()
                    },
                )

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request, systembruker())

            with(resultat) {
                opprettetRevurdering shouldBe true
                opprettetOppgave shouldBe false
                oppgaveId shouldBe null
                nyBehandlingId shouldBe revurdering.id
                forrigeBehandlingId shouldBe forrigeBehandling.id
            }
            verify(exactly = 1) { oppgaveService.fjernSaksbehandler(any()) }
        }

        @Test
        fun `Skal opprette oppgave hvis det finnes en aapen behandling og kravene ikke er oppfylt`() {
            val oppgave =
                mockk<OppgaveIntern> {
                    every { id } returns UUID.randomUUID()
                }
            val aapenBehandling =
                mockk<Behandling> {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                }
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns emptyList()
            every { behandlingService.hentSisteIverksatte(sakId) } returns forrigeBehandling
            every { behandlingService.hentBehandlingerForSak(sakId) } returns listOf(forrigeBehandling, aapenBehandling)
            coEvery { grunnlagKlient.hentPersongalleri(forrigeBehandling.id, any()) } returns persongalleriOpplysning
            every {
                oppgaveService.opprettOppgave(
                    sakId = sakId,
                    referanse = any(),
                    kilde = OppgaveKilde.HENDELSE,
                    type = OppgaveType.AKTIVITETSPLIKT_REVURDERING,
                    merknad = JobbType.OMS_DOED_6MND.beskrivelse,
                    frist = frist,
                )
            } returns oppgave

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request, systembruker())

            with(resultat) {
                opprettetRevurdering shouldBe false
                opprettetOppgave shouldBe true
                oppgaveId shouldBe oppgave.id
                nyBehandlingId shouldBe null
                forrigeBehandlingId shouldBe forrigeBehandling.id // ??
            }
            verify { revurderingService wasNot Called }
        }

        @Test
        fun `Skal ikke opprette revurdering eller oppgave hvis kravene for aktivitetsplikt er oppfylt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                listOf(
                    mockk {
                        every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
                        every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                        every { sakId } returns aktivitet.sakId
                        every { fom } returns LocalDate.of(2024, Month.APRIL, 1)
                        every { behandlingId } returns UUID.randomUUID()
                        every { oppgaveId } returns null
                    },
                )
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns emptyList()
            every { behandlingService.hentSisteIverksatte(sakId) } returns forrigeBehandling
            coEvery { grunnlagKlient.hentPersongalleri(forrigeBehandling.id, any()) } returns persongalleriOpplysning

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request, systembruker())

            with(resultat) {
                opprettetRevurdering shouldBe false
                opprettetOppgave shouldBe false
                oppgaveId shouldBe null
                nyBehandlingId shouldBe null
                forrigeBehandlingId shouldBe forrigeBehandling.id
            }
            verify { revurderingService wasNot Called }
            verify { oppgaveService wasNot Called }
        }
    }

    @Nested
    inner class OpprettOppgaveHvisVarigUnntak {
        private val sakId = sakId1
        private val frist = Tidspunkt.now()
        private val request =
            OpprettOppgaveForAktivitetspliktVarigUnntakDto(
                sakId = sakId,
                frist = frist,
                jobbType = JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK,
            )

        @Test
        fun `Skal ikke opprette oppgave hvis det ikke er varig unntak`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns emptyList()

            val resultat = service.opprettOppgaveHvisVarigUnntak(request)

            with(resultat) {
                opprettetOppgave shouldBe false
                oppgaveId shouldBe null
            }
            verify { oppgaveService wasNot Called }
        }

        @Test
        fun `Skal opprette oppgave hvis det er varig unntak`() {
            val behandlingsId = UUID.randomUUID()
            val oppgave =
                mockk<OppgaveIntern> {
                    every { id } returns UUID.randomUUID()
                }

            every {
                oppgaveService.opprettOppgave(
                    sakId = sakId,
                    referanse = any(),
                    kilde = OppgaveKilde.HENDELSE,
                    type = OppgaveType.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK,
                    merknad = JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK.beskrivelse,
                    frist = frist,
                )
            } returns oppgave
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns emptyList()
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns
                listOf(
                    mockk {
                        every { unntak } returns AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                        every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                        every { sakId } returns aktivitet.sakId
                        every { behandlingId } returns behandlingsId
                        every { oppgaveId } returns null
                    },
                )
            val resultat = service.opprettOppgaveHvisVarigUnntak(request)

            with(resultat) {
                opprettetOppgave shouldBe true
                oppgaveId shouldBe oppgave.id
            }
        }
    }

    companion object {
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.VILKAARSVURDERT
                every { sak } returns
                    mockk {
                        every { id } returns sakId1
                    }
            }
        val aktivitet =
            LagreAktivitetspliktAktivitet(
                sakId = sakId1,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )
    }
}
