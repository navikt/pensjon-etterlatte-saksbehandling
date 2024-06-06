package behandling.aktivitetsplikt

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitet
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitetType
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.aktivitetsplikt.LagreAktivitetspliktAktivitet
import no.nav.etterlatte.behandling.aktivitetsplikt.SakidTilhoererIkkeBehandlingException
import no.nav.etterlatte.behandling.aktivitetsplikt.TomErFoerFomException
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
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto.JobbType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
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
    private val service =
        AktivitetspliktService(
            aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao,
            behandlingService,
            grunnlagKlient,
            automatiskRevurderingService,
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

            service.slettAktivitet(behandling.id, aktivitetId)

            coVerify { aktivitetspliktDao.slettAktivitet(aktivitetId, behandling.id) }
        }

        @Test
        fun `Skal kaste feil hvis behandling ikke kan endres`() {
            every { behandlingService.hentBehandling(behandling.id) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.FATTET_VEDTAK
                }

            assertThrows<BehandlingKanIkkeEndres> {
                service.slettAktivitet(behandling.id, aktivitetId)
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

            service.opprettUnntakForBehandling(unntak, behandlingId, sakId, brukerTokenInfo)

            verify { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) }
            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
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
            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns mockk()

            assertThrows<IllegalArgumentException> {
                service.opprettUnntakForBehandling(unntak, behandlingId, sakId, brukerTokenInfo)
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
    inner class OppfyllerAktivitetsplikt {
        @Test
        fun `Skal returnere true hvis aktivitetsplikt er oppfylt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
                }

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe true
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt og det ikke finnes unntak`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                    every { fom } returns aktivitet.fom.minusMonths(1)
                }
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns null

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe false
        }

        @Test
        fun `Skal returnere true hvis aktivitetsplikt ikke er oppfylt, men det finnes unntak`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                    every { fom } returns aktivitet.fom.minusMonths(1)
                }
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                mockk {
                    every { tom } returns null
                }

            val result = service.oppfyllerAktivitetsplikt(aktivitet.sakId, aktivitet.fom)

            result shouldBe true
        }

        @Test
        fun `Skal returnere false hvis aktivitetsplikt ikke er oppfylt og unntaket er utgaatt`() {
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns null
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                mockk {
                    every { tom } returns LocalDate.now().minusYears(1)
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
                    virkningstidspunkt = request.behandlingsmaaned.atDay(1),
                    kilde = Vedtaksloesning.GJENNY,
                    persongalleri = persongalleri,
                    frist = frist,
                    begrunnelse = request.jobbType.beskrivelse,
                )
            } returns
                mockk { every { oppdater() } returns revurdering }

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request)

            with(resultat) {
                opprettetRevurdering shouldBe true
                opprettetOppgave shouldBe false
                oppgaveId shouldBe null
                nyBehandlingId shouldBe revurdering.id
                forrigeBehandlingId shouldBe forrigeBehandling.id
            }
            verify { oppgaveService wasNot Called }
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
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    sakId = sakId,
                    referanse = any(),
                    oppgaveKilde = OppgaveKilde.HENDELSE,
                    oppgaveType = OppgaveType.AKTIVITETSPLIKT_REVURDERING,
                    merknad = JobbType.OMS_DOED_6MND.beskrivelse,
                    frist = frist,
                )
            } returns oppgave

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request)

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
                }
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId) } returns null
            every { behandlingService.hentSisteIverksatte(sakId) } returns forrigeBehandling
            coEvery { grunnlagKlient.hentPersongalleri(forrigeBehandling.id, any()) } returns persongalleriOpplysning

            val resultat = service.opprettRevurderingHvisKravIkkeOppfylt(request)

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
