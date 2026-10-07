package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.equals.shouldNotEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.BeregnFaktiskInntektRequest
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.FantIkkeEtteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak
import no.nav.etterlatte.sak.SakLesDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerForbehandlingServiceTest {
    private class TestContext {
        val dao: EtteroppgjoerForbehandlingDao = mockk()
        val sakDao: SakLesDao = mockk()
        val etteroppgjoerService: EtteroppgjoerService = mockk()
        val oppgaveService: OppgaveService = mockk()
        val inntektskomponentService: InntektskomponentService = mockk()
        val pensjonsgivendeInntektService: PensjonsgivendeInntektService = mockk()
        val hendelserService: EtteroppgjoerForbehandlingHendelseService = mockk()
        val beregningKlient: BeregningKlient = mockk()
        val behandlingService: BehandlingService = mockk()
        val vedtakInternalService: VedtakInternalService = mockk()
        val featureToggleService: FeatureToggleService = mockk()
        val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService = EtteroppgjoerOppgaveService(oppgaveService)
        val etteroppgjoerDataService: EtteroppgjoerDataService =
            EtteroppgjoerDataService(behandlingService, vedtakInternalService, beregningKlient)

        val service =
            EtteroppgjoerForbehandlingService(
                dao = dao,
                sakDao = sakDao,
                etteroppgjoerService = etteroppgjoerService,
                oppgaveService = oppgaveService,
                inntektskomponentService = inntektskomponentService,
                pensjonsgivendeInntektService = pensjonsgivendeInntektService,
                hendelserService = hendelserService,
                beregningKlient = beregningKlient,
                behandlingService = behandlingService,
                vedtakInternalService = vedtakInternalService,
                etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
                etteroppgjoerDataService = etteroppgjoerDataService,
                featureToggleService = featureToggleService,
            )

        val behandling =
            foerstegangsbehandling(
                sakId = sakId1,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.IVERKSATT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )

        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId1,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                harSanksjon = false,
                harOpphoer = false,
                harAdressebeskyttelseEllerSkjermet = false,
                harAktivitetskrav = false,
                harBosattUtland = false,
                harOverstyrtBeregning = false,
                sisteFerdigstilteForbehandling = UUID.randomUUID(),
            )

        val oppgaveId = UUID.randomUUID()

        init {
            coEvery {
                behandlingService.hentSisteIverksatteBehandling(sakId1)
            } returns behandling

            coEvery { sakDao.hentSak(any()) } returns sak(sakId = sakId1, sakType = SakType.OMSTILLINGSSTOENAD)
            coEvery { oppgaveService.hentOppgave(any()) } returns
                mockk {
                    every { sakId } returns sakId1
                    every { erAvsluttet() } returns false
                    every { type } returns OppgaveType.ETTEROPPGJOER
                }
            coEvery { oppgaveService.hentOppgaverForSak(any()) } returns emptyList()
            coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any()) } returns etteroppgjoer

            every { dao.lagreForbehandling(any()) } returns 1
            every { dao.kopierSummerteInntekter(any(), any()) } returns 1
            every { dao.kopierPensjonsgivendeInntekt(any(), any()) } returns 1
        }

        fun returnsForbehandling(forbehandling: EtteroppgjoerForbehandling) {
            coEvery {
                dao.hentForbehandling(any())
            } returns forbehandling
        }

        fun returnsForbehandlinger(forbehandlinger: List<EtteroppgjoerForbehandling>) {
            coEvery {
                dao.hentForbehandlingerForSak(any())
            } returns forbehandlinger
        }

        fun returnsOppgave(oppgave: OppgaveIntern) {
            coEvery { oppgaveService.hentOppgave(any()) } returns oppgave
        }

        fun returnsSak(sak: Sak) {
            coEvery { sakDao.hentSak(any()) } returns sak
        }

        fun returnsEtteroppgjoer(etteroppgjoer: Etteroppgjoer) {
            coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any()) } returns etteroppgjoer
        }

        fun faktiskInntektRequest() =
            BeregnFaktiskInntektRequest(
                loennsinntekt = 100,
                afp = 0,
                naeringsinntekt = 0,
                utlandsinntekt = 0,
                spesifikasjon = "",
            )

        fun stubLagreOgBeregnFaktiskInntekt(
            forbehandling: EtteroppgjoerForbehandling,
            vararg ekstraForbehandlinger: EtteroppgjoerForbehandling,
        ): io.mockk.CapturingSlot<EtteroppgjoerBeregnFaktiskInntektRequest> {
            (listOf(forbehandling) + ekstraForbehandlinger).forEach {
                coEvery { dao.hentForbehandling(it.id) } returns it
            }
            coEvery { vedtakInternalService.hentInnvilgedePerioder(any(), any()) } returns
                listOf(
                    InnvilgetPeriodeDto(
                        periode =
                            no.nav.etterlatte.libs.common.vedtak.Periode(
                                fom = forbehandling.innvilgetPeriode.fom,
                                tom = forbehandling.innvilgetPeriode.tom,
                            ),
                        vedtak = emptyList(),
                    ),
                )
            every { behandlingService.hentBehandlingerForSak(any()) } returns emptyList()
            every { behandlingService.hentUtlandstilknytningForSak(any()) } returns null
            every { hendelserService.registrerOgSendHendelse(any(), any(), any(), any(), any()) } returns Unit

            val resultat =
                mockk<BeregnetEtteroppgjoerResultatDto> {
                    every { resultatType } returns EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING
                }
            val requestSlot = slot<EtteroppgjoerBeregnFaktiskInntektRequest>()
            coEvery {
                beregningKlient.beregnAvkortingFaktiskInntekt(capture(requestSlot), any())
            } returns resultat
            return requestSlot
        }
    }

    @Test
    fun `skal hente siste iverksatte behandling med avkorting`() {
        val ctx = TestContext()

        val behandling =
            foerstegangsbehandling(
                sakId = sakId1,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.IVERKSATT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )

        val revurdering =
            revurdering(
                sakId = sakId1,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                revurderingAarsak = Revurderingaarsak.ANNEN,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )

        val underBehandling =
            revurdering(
                sakId = sakId1,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.BEREGNET,
                revurderingAarsak = Revurderingaarsak.ANNEN,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )

        coEvery { ctx.vedtakInternalService.hentIverksatteVedtak(sakId1, any()) } returns
            listOf(
                VedtakSammendragDto(
                    id = "1",
                    behandlingId = behandling.id,
                    vedtakType = VedtakType.INNVILGELSE,
                    behandlendeSaksbehandler = "saksbehandler",
                    datoFattet = Tidspunkt.now().toNorskTid(),
                    attesterendeSaksbehandler = "attestant",
                    datoAttestert = Tidspunkt.now().toNorskTid(),
                    virkningstidspunkt = behandling.virkningstidspunkt?.dato!!,
                    opphoerFraOgMed = null,
                    iverksettelsesTidspunkt = Tidspunkt.now(),
                ),
            )

        coEvery { ctx.behandlingService.hentBehandlingerForSak(sakId1) } returns
            listOf(behandling, revurdering, underBehandling)

        val vedtakListe = ctx.etteroppgjoerDataService.hentIverksatteVedtak(sakId1, mockk())
        val sisteVedtakMedAvkorting = ctx.etteroppgjoerDataService.sisteVedtakMedAvkorting(vedtakListe)
        val vedtakMedGjeldendeOpphoer = ctx.etteroppgjoerDataService.vedtakMedGjeldendeOpphoer(vedtakListe)

        sisteVedtakMedAvkorting.behandlingId shouldBe behandling.id
        vedtakMedGjeldendeOpphoer shouldBe null
    }

    @ParameterizedTest(name = "skal ikke opprette forbehandling hvis det allerede eksisterer en med status={0}")
    @EnumSource(
        value = EtteroppgjoerForbehandlingStatus::class,
        names = ["FERDIGSTILT", "AVBRUTT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal ikke opprette forbehandling hvis det eksisterer en fra før og den er under behandling`(
        status: EtteroppgjoerForbehandlingStatus,
    ) {
        val ctx = TestContext()

        ctx.returnsForbehandlinger(
            listOf(
                EtteroppgjoerForbehandling
                    .opprett(
                        sak(),
                        Periode(YearMonth.now().minusYears(1), null),
                        ctx.behandling.id,
                    ).copy(aar = 2024, status = status),
            ),
        )

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                ctx.service.opprettEtteroppgjoerForbehandling(
                    sakId1,
                    2024,
                    ctx.oppgaveId,
                    mockk(),
                )
            }

        assertEquals(exception.code, "FORBEHANDLING_FINNES_ALLEREDE")
    }

    @Test
    fun `harForbehandlingForAar returnerer false når kun andre år finnes`() {
        val ctx = TestContext()
        ctx.returnsForbehandlinger(
            listOf(
                EtteroppgjoerForbehandling
                    .opprett(
                        sak(),
                        Periode(YearMonth.now().minusYears(1), null),
                        ctx.behandling.id,
                    ).copy(aar = 2023),
            ),
        )

        ctx.service.harForbehandlingForAar(sakId1, 2024) shouldBe false
    }

    @Test
    fun `harForbehandlingForAar returnerer true når samme år finnes`() {
        val ctx = TestContext()
        ctx.returnsForbehandlinger(
            listOf(
                EtteroppgjoerForbehandling
                    .opprett(
                        sak(),
                        Periode(YearMonth.now().minusYears(1), null),
                        ctx.behandling.id,
                    ).copy(aar = 2024),
            ),
        )

        ctx.service.harForbehandlingForAar(sakId1, 2024) shouldBe true
    }

    @ParameterizedTest(name = "skal ikke opprette forbehandling for status={0}")
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["MOTTATT_SKATTEOPPGJOER", "MANGLER_SKATTEOPPGJOER"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal ikke opprette forbehandling hvis etteroppgjoer ikke har rett status status`(status: EtteroppgjoerStatus) {
        val ctx = TestContext()

        ctx.returnsEtteroppgjoer(ctx.etteroppgjoer.copy(status = status))

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                ctx.service.opprettEtteroppgjoerForbehandling(
                    sakId1,
                    2024,
                    ctx.oppgaveId,
                    mockk(),
                )
            }

        assertEquals(exception.code, "FEIL_ETTEROPPGJOERS_STATUS")
    }

    @Test
    fun `skal ikke opprette forbehandling hvis etteroppgjoer ikke finnes`() {
        val ctx = TestContext()

        coEvery {
            ctx.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any())
        } throws FantIkkeEtteroppgjoer(sakId1, 2024)

        val exception =
            assertThrows(FantIkkeEtteroppgjoer::class.java) {
                ctx.service.opprettEtteroppgjoerForbehandling(
                    sakId1,
                    2024,
                    ctx.oppgaveId,
                    mockk(),
                )
            }

        assertEquals(exception.code, "MANGLER_ETTEROPPGJOER")
    }

    @Test
    fun `skal ikke opprette forbehandling hvis ikke sakType er OMS`() {
        val ctx = TestContext()

        ctx.returnsSak(sak(sakId = sakId1, sakType = SakType.BARNEPENSJON))

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                ctx.service.opprettEtteroppgjoerForbehandling(
                    sakId1,
                    2024,
                    ctx.oppgaveId,
                    mockk(),
                )
            }

        assertEquals(exception.code, "FEIL_SAKTYPE")
    }

    @Test
    fun `skal ikke opprette forbehandling hvis oppgave for opprette forbehandling ikke er gyldig`() {
        val ctx = TestContext()

        ctx.returnsOppgave(
            mockk {
                every { sakId } returns sakId1
                every { erAvsluttet() } returns false
                every { type } returns OppgaveType.FOERSTEGANGSBEHANDLING
            },
        )

        assertThrows(UgyldigForespoerselException::class.java) {
            ctx.service.opprettEtteroppgjoerForbehandling(
                sakId1,
                2024,
                ctx.oppgaveId,
                mockk(),
            )
        }
    }

    @Test
    fun `skal kopiere forbehandling, summerteInntekter og pensjonsgivendeInntekt ved kopierOgLagreNyForbehandling`() {
        val ctx = TestContext()
        val uuid = UUID.randomUUID()

        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.now().minusYears(1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                ).copy(
                    brevId = 123L,
                    varselbrevSendt = LocalDate.now(),
                    klageOmgjoering = UUID.randomUUID(),
                )

        ctx.returnsForbehandling(forbehandling)
        coEvery { ctx.vedtakInternalService.hentIverksatteVedtak(sakId1, any()) } returns
            listOf(
                VedtakSammendragDto(
                    id = "1",
                    behandlingId = ctx.behandling.id,
                    vedtakType = VedtakType.INNVILGELSE,
                    behandlendeSaksbehandler = "saksbehandler",
                    datoFattet = Tidspunkt.now().toNorskTid(),
                    attesterendeSaksbehandler = "attestant",
                    datoAttestert = Tidspunkt.now().toNorskTid(),
                    virkningstidspunkt = ctx.behandling.virkningstidspunkt?.dato!!,
                    opphoerFraOgMed = null,
                    iverksettelsesTidspunkt = Tidspunkt.now(),
                ),
            )
        every { ctx.behandlingService.hentBehandlingerForSak(any()) } returns listOf(ctx.behandling)

        val nyKlageOmgjoering = UUID.randomUUID()
        val kopiertForbehandling =
            ctx.service.kopierOgLagreNyForbehandling(
                forbehandlingId = uuid,
                sakId = sakId1,
                brukerTokenInfo = mockk(),
                klageId = nyKlageOmgjoering,
                omgjoeringEgetInitiativ = false,
            )

        with(kopiertForbehandling) {
            id shouldNotEqual forbehandling.id
            kopiertFra shouldBe forbehandling.id
            sisteIverksatteBehandlingId shouldBe ctx.behandling.id
            brevId shouldBe null
            varselbrevSendt shouldBe null
            klageOmgjoering shouldBe nyKlageOmgjoering
        }

        verify {
            ctx.dao.lagreForbehandling(kopiertForbehandling)
            ctx.dao.kopierSummerteInntekter(forbehandling.id, kopiertForbehandling.id)
            ctx.dao.kopierPensjonsgivendeInntekt(forbehandling.id, kopiertForbehandling.id)
        }
    }

    @Test
    fun `ved klage-omgjoering sammenlignes det mot baseline fra forbehandlingen som omgjoeres`() {
        val ctx = TestContext()

        val baselineBehandlingId = UUID.randomUUID()
        val omgjortForbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                    sisteIverksatteBehandling = baselineBehandlingId,
                )
        val omgjoeringForbehandling =
            omgjortForbehandling.copy(
                id = UUID.randomUUID(),
                kopiertFra = omgjortForbehandling.id,
                klageOmgjoering = UUID.randomUUID(),
                sisteIverksatteBehandlingId = UUID.randomUUID(),
            )

        val request = ctx.stubLagreOgBeregnFaktiskInntekt(omgjoeringForbehandling, omgjortForbehandling)

        ctx.service.lagreOgBeregnFaktiskInntekt(
            omgjoeringForbehandling.id,
            ctx.faktiskInntektRequest(),
            simpleSaksbehandler(),
        )

        // Baseline = den omgjorte forbehandlingens sisteIverksatteBehandling (ytelsen før det opprinnelige etteroppgjøret)
        request.captured.sammenlignTilOgMedBehandlingId shouldBe baselineBehandlingId
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `flerperiode etteroppgjoer krever aktiv toggle foer inntekt beregnes`(toggleAktiv: Boolean) {
        val ctx = TestContext()
        every {
            ctx.featureToggleService.isEnabled(EtteroppgjoerToggles.BEREGN_OVER_FLERE_PERIODER, false)
        } returns toggleAktiv
        val januar = YearMonth.of(2024, 1)
        val mars = YearMonth.of(2024, 3)
        val juli = YearMonth.of(2024, 7)
        val desember = YearMonth.of(2024, 12)
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = ctx.behandling.sak,
                innvilgetPeriode = Periode(januar, desember),
                sisteIverksatteBehandling = ctx.behandling.id,
            )
        val request = ctx.stubLagreOgBeregnFaktiskInntekt(forbehandling)
        coEvery { ctx.vedtakInternalService.hentInnvilgedePerioder(any(), any()) } returns
            listOf(
                InnvilgetPeriodeDto(
                    no.nav.etterlatte.libs.common.vedtak
                        .Periode(januar, mars),
                    emptyList(),
                ),
                InnvilgetPeriodeDto(
                    no.nav.etterlatte.libs.common.vedtak
                        .Periode(juli, null),
                    emptyList(),
                ),
            )

        if (toggleAktiv) {
            ctx.service.lagreOgBeregnFaktiskInntekt(forbehandling.id, ctx.faktiskInntektRequest(), simpleSaksbehandler())

            request.captured.innvilgetPeriodeIEtteroppgjoersAar shouldBe Periode(januar, desember)
            request.captured.innvilgedePerioderIEtteroppgjoersAar shouldBe
                listOf(Periode(januar, mars), Periode(juli, desember))
            request.captured.opphoerFom shouldBe null
        } else {
            val feil =
                assertThrows(UgyldigForespoerselException::class.java) {
                    ctx.service.lagreOgBeregnFaktiskInntekt(forbehandling.id, ctx.faktiskInntektRequest(), simpleSaksbehandler())
                }

            feil.code shouldBe "FLERPERIODEBEREGNING_IKKE_AKTIV"
            request.isCaptured shouldBe false
            verify(exactly = 0) { ctx.dao.lagreForbehandling(any()) }
        }
    }

    @Test
    fun `opprettelse av flerperiode forbehandling blokkeres uten lagring naar toggle er av`() {
        val ctx = TestContext()
        ctx.returnsForbehandlinger(emptyList())
        every {
            ctx.featureToggleService.isEnabled(EtteroppgjoerToggles.BEREGN_OVER_FLERE_PERIODER, false)
        } returns false
        coEvery { ctx.vedtakInternalService.hentIverksatteVedtak(any(), any()) } returns
            listOf(
                mockk<VedtakSammendragDto> {
                    every { vedtakType } returns VedtakType.INNVILGELSE
                    every { datoAttestert } returns null
                    every { behandlingId } returns ctx.behandling.id
                    every { opphoerFraOgMed } returns null
                },
            )
        coEvery { ctx.beregningKlient.harAvkortingMedSanksjonGamleRegler(any(), any(), any()) } returns
            mockk { every { harGammelBeregningMedSanksjon } returns false }
        coEvery { ctx.vedtakInternalService.hentInnvilgedePerioder(any(), any()) } returns
            listOf(
                InnvilgetPeriodeDto(
                    no.nav.etterlatte.libs.common.vedtak
                        .Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 3)),
                    emptyList(),
                ),
                InnvilgetPeriodeDto(
                    no.nav.etterlatte.libs.common.vedtak
                        .Periode(YearMonth.of(2024, 7), null),
                    emptyList(),
                ),
            )

        val feil =
            assertThrows(UgyldigForespoerselException::class.java) {
                ctx.service.opprettEtteroppgjoerForbehandling(sakId1, 2024, ctx.oppgaveId, simpleSaksbehandler())
            }

        feil.code shouldBe "FLERPERIODEBEREGNING_IKKE_AKTIV"
        verify(exactly = 0) { ctx.dao.lagreForbehandling(any()) }
        verify(exactly = 0) { ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(any(), any(), any()) }
    }

    @Test
    fun `endret oppgjoersperiode blokkerer beregning paa gammel forbehandling`() {
        val ctx = TestContext()
        val januar = YearMonth.of(2024, 1)
        val mars = YearMonth.of(2024, 3)
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = ctx.behandling.sak,
                innvilgetPeriode = Periode(januar, YearMonth.of(2024, 12)),
                sisteIverksatteBehandling = ctx.behandling.id,
            )
        val request = ctx.stubLagreOgBeregnFaktiskInntekt(forbehandling)
        coEvery { ctx.vedtakInternalService.hentInnvilgedePerioder(any(), any()) } returns
            listOf(
                InnvilgetPeriodeDto(
                    no.nav.etterlatte.libs.common.vedtak
                        .Periode(januar, mars),
                    emptyList(),
                ),
            )

        assertThrows(UgyldigForespoerselException::class.java) {
            ctx.service.lagreOgBeregnFaktiskInntekt(forbehandling.id, ctx.faktiskInntektRequest(), simpleSaksbehandler())
        }

        request.isCaptured shouldBe false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `en innvilget periode fungerer med toggle av og paa uten klage-omgjoering`(toggleAktiv: Boolean) {
        val ctx = TestContext()
        every {
            ctx.featureToggleService.isEnabled(EtteroppgjoerToggles.BEREGN_OVER_FLERE_PERIODER, false)
        } returns toggleAktiv

        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = ctx.behandling.sak,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                sisteIverksatteBehandling = UUID.randomUUID(),
            )

        val request = ctx.stubLagreOgBeregnFaktiskInntekt(forbehandling)

        ctx.service.lagreOgBeregnFaktiskInntekt(forbehandling.id, ctx.faktiskInntektRequest(), simpleSaksbehandler())

        request.captured.sammenlignTilOgMedBehandlingId shouldBe null
    }
}
