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
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto.JobbType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class AktivitetspliktServiceTest {
    private val aktivitetspliktDao: AktivitetspliktDao = mockk()
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao = mockk()
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val automatiskRevurderingService: AutomatiskRevurderingService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val statistikkProduer: BehandlingHendelserKafkaProducer = mockk()
    private val service =
        AktivitetspliktService(
            aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao,
            behandlingService,
            grunnlagKlient,
            automatiskRevurderingService,
            statistikkProduer,
            oppgaveService,
        )
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
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
            every { aktivitetspliktDao.hentAktiviteter(behandlingId) } returns listOf(aktivitet)

            val result = service.hentAktiviteter(behandlingId)

            result shouldBe listOf(aktivitet)
        }
    }

    @Nested
    inner class OpprettEllerOppdaterAktivitet {
        @Test
        fun `Skal opprette en aktivitet`() {
            every { aktivitetspliktDao.opprettAktivitet(behandling.id, aktivitet, any()) } returns 1

            service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)

            coVerify { aktivitetspliktDao.opprettAktivitet(behandling.id, aktivitet, any()) }
            coVerify(exactly = 0) { aktivitetspliktDao.oppdaterAktivitet(any(), any(), any()) }
        }

        @Test
        fun `Skal oppdatere en aktivitet`() {
            val aktivitet = aktivitet.copy(id = UUID.randomUUID())
            every { aktivitetspliktDao.oppdaterAktivitet(behandling.id, aktivitet, any()) } returns 1

            service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)

            coVerify { aktivitetspliktDao.oppdaterAktivitet(behandling.id, aktivitet, any()) }
            coVerify(exactly = 0) { aktivitetspliktDao.opprettAktivitet(any(), any(), any()) }
        }

        @Test
        fun `Skal kaste feil hvis sakId ikke stemmer overens med behandling`() {
            val aktivitet = aktivitet.copy(sakId = 2)

            assertThrows<SakidTilhoererIkkeBehandlingException> {
                service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal kaste feil hvis tom er foer fom`() {
            val aktivitet = aktivitet.copy(tom = LocalDate.now().minusYears(1))

            assertThrows<TomErFoerFomException> {
                service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)
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
                service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)
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

            service.slettAktivitet(behandling.id, aktivitetId, brukerTokenInfo)

            coVerify { aktivitetspliktDao.slettAktivitet(aktivitetId, behandling.id) }
        }

        @Test
        fun `Skal kaste feil hvis behandling ikke kan endres`() {
            every { behandlingService.hentBehandling(behandling.id) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.FATTET_VEDTAK
                }

            assertThrows<BehandlingKanIkkeEndres> {
                service.slettAktivitet(behandling.id, aktivitetId, brukerTokenInfo)
            }
        }
    }

    @Nested
    inner class OpprettOgHentAktivitetsgrad {
        private val oppgaveId = UUID.randomUUID()
        private val sakId = 1L

        @Test
        fun `Skal opprette en ny aktivitetsgrad`() {
            val aktivitetsgrad =
                LagreAktivitetspliktAktivitetsgrad(
                    aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                    beskrivelse = "Beskrivelse",
                )
            every { aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(aktivitetsgrad, sakId, any(), oppgaveId) } returns 1
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns null
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
            every { aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(aktivitetsgrad, sakId, any(), oppgaveId) } returns 1
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns mockk()

            assertThrows<IllegalArgumentException> {
                service.opprettAktivitetsgradForOppgave(aktivitetsgrad, oppgaveId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal hente en vurdering med aktivitetsgrad basert paa oppgaveId`() {
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns mockk()
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns null

            val vurdering = service.hentVurderingForOppgave(oppgaveId)

            vurdering shouldNotBe null
            vurdering?.aktivitet shouldNotBe null
            vurdering?.unntak shouldBe null

            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) }
        }
    }

    @Nested
    inner class OpprettOgHentUnntakForOppgave {
        private val oppgaveId = UUID.randomUUID()
        private val sakId = 1L

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
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns null
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
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns mockk()

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
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns null

            assertThrows<TomErFoerFomException> {
                service.opprettUnntakForOpppgave(unntak, oppgaveId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal hente en vurdering med unntak basert paa oppgaveId`() {
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) } returns null
            every { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) } returns mockk()

            val vurdering = service.hentVurderingForOppgave(oppgaveId)

            vurdering shouldNotBe null
            vurdering?.aktivitet shouldBe null
            vurdering?.unntak shouldNotBe null

            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId) }
        }
    }

    @Nested
    inner class OpprettOgHentUnntakForBehandling {
        private val behandlingId = UUID.randomUUID()
        private val sakId = 1L

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
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns null
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns null
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
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns null
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
                AktivitetspliktAktivitetsgrad(
                    id = aktivitetsgradId,
                    sakId = sakId,
                    behandlingId = behandlingId,
                    oppgaveId = null,
                    aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                    fom = LocalDate.now(),
                    opprettet = kilde,
                    endret = kilde,
                    beskrivelse = "Beskrivelse",
                )

            aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(aktivitetsgrad, sakId, kilde, behandlingId = behandlingId)
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
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns mockk()
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
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns null

            assertThrows<TomErFoerFomException> {
                service.opprettUnntakForOpppgave(unntak, behandlingId, sakId, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal hente en vurdering med unntak basert paa behandlingId`() {
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns null
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns mockk()

            val vurdering = service.hentVurderingForBehandling(behandlingId)

            vurdering shouldNotBe null
            vurdering?.aktivitet shouldBe null
            vurdering?.unntak shouldNotBe null

            verify { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
        }
    }

    @Nested
    inner class OppdaterUnntakForBehandling {
        private val behandlingId = UUID.randomUUID()
        private val sakId = 1L

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
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns null
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns null
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
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
                    every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                    every { sakId } returns aktivitet.sakId
                }
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns null

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe true
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt og det ikke finnes unntak`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                    every { fom } returns aktivitet.fom.minusMonths(1)
                    every { sakId } returns aktivitet.sakId
                }
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns null

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe false
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt, selv om det finnes eldre unntak`() {
            val foerst = Grunnlagsopplysning.Saksbehandler.create("Z123455")
            val sist = Grunnlagsopplysning.Saksbehandler.create("Z123455")
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                mockk {
                    every { tom } returns null
                    every { opprettet } returns foerst
                    every { sakId } returns aktivitet.sakId
                }
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                    every { fom } returns aktivitet.fom.minusMonths(1)
                    every { opprettet } returns sist
                    every { sakId } returns aktivitet.sakId
                }

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe false
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt og unntaket er utgaatt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns null
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                mockk {
                    every { tom } returns LocalDate.now().minusYears(1)
                    every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                    every { sakId } returns aktivitet.sakId
                }

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe false
        }
    }

    @Nested
    inner class OpprettRevurderingHvisKravIkkeOppfylt {
        private val sakId = 1L
        private val forrigeBehandling: Behandling =
            mockk {
                every { id } returns UUID.randomUUID()
                every { status } returns BehandlingStatus.IVERKSATT
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
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId) } returns null
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns null
            every { behandlingService.hentSisteIverksatte(sakId) } returns forrigeBehandling
            every { behandlingService.hentBehandlingerForSak(sakId) } returns listOf(forrigeBehandling)
            coEvery { grunnlagKlient.hentPersongalleri(forrigeBehandling.id, any()) } returns persongalleriOpplysning
            every {
                automatiskRevurderingService.opprettAutomatiskRevurdering(
                    sakId = sakId,
                    forrigeBehandling = forrigeBehandling,
                    revurderingAarsak = Revurderingaarsak.AKTIVITETSPLIKT,
                    virkningstidspunkt = request.behandlingsmaaned.atDay(1).plusMonths(1),
                    kilde = Vedtaksloesning.GJENNY,
                    persongalleri = persongalleri,
                    frist = frist,
                    begrunnelse = request.jobbType.beskrivelse,
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

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request, Systembruker.automatiskJobb)

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
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId) } returns null
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns null
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

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request, Systembruker.automatiskJobb)

            with(resultat) {
                opprettetRevurdering shouldBe false
                opprettetOppgave shouldBe true
                oppgaveId shouldBe oppgave.id
                nyBehandlingId shouldBe null
                forrigeBehandlingId shouldBe forrigeBehandling.id // ??
            }
            verify { automatiskRevurderingService wasNot Called }
        }

        @Test
        fun `Skal ikke opprette revurdering eller oppgave hvis kravene for aktivitetsplikt er oppfylt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
                    every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                    every { sakId } returns aktivitet.sakId
                }
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns null
            every { behandlingService.hentSisteIverksatte(sakId) } returns forrigeBehandling
            coEvery { grunnlagKlient.hentPersongalleri(forrigeBehandling.id, any()) } returns persongalleriOpplysning

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request, Systembruker.automatiskJobb)

            with(resultat) {
                opprettetRevurdering shouldBe false
                opprettetOppgave shouldBe false
                oppgaveId shouldBe null
                nyBehandlingId shouldBe null
                forrigeBehandlingId shouldBe forrigeBehandling.id
            }
            verify { automatiskRevurderingService wasNot Called }
            verify { oppgaveService wasNot Called }
        }
    }

    companion object {
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.VILKAARSVURDERT
                every { sak } returns
                    mockk {
                        every { id } returns 1L
                    }
            }
        val aktivitet =
            LagreAktivitetspliktAktivitet(
                sakId = 1L,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )
    }
}
