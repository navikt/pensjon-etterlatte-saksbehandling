package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.ForbehandlingKanIkkeEndres
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelser
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak
import no.nav.etterlatte.sak.SakLesDao
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
        val vedtakKlient: VedtakKlient = mockk()
        val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService = EtteroppgjoerOppgaveService(oppgaveService)
        val etteroppgjoerDataService: EtteroppgjoerDataService =
            EtteroppgjoerDataService(vedtakKlient, beregningKlient)

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
                vedtakKlient = vedtakKlient,
                etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
                etteroppgjoerDataService = etteroppgjoerDataService,
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
                harInstitusjonsopphold = false,
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
            every { oppgaveService.hentOppgaverForSakAvType(any(), any()) } returns emptyList()
            every {
                oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns mockk(relaxed = true)
            every { oppgaveService.hentOppgaverForReferanse(any()) } returns emptyList()
            coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any()) } returns etteroppgjoer
            every { behandlingService.hentUtlandstilknytningForSak(any()) } returns null

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

        fun returnsOppgaverForReferanse(oppgaver: List<OppgaveIntern>) {
            every { oppgaveService.hentOppgaverForReferanse(any()) } returns oppgaver
        }

        fun returnsSak(sak: Sak) {
            coEvery { sakDao.hentSak(any()) } returns sak
        }

        fun returnsEtteroppgjoer(etteroppgjoer: Etteroppgjoer) {
            coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any()) } returns etteroppgjoer
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

        coEvery { ctx.vedtakKlient.hentIverksatteVedtak(sakId1, any()) } returns
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
                        mottattSkatteoppgjoer = true,
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
    fun `skal opprette manuell oppgave for opprett forbehandling`() {
        val ctx = TestContext()

        justRun {
            ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sakId1,
                2024,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )
        }

        ctx.service.opprettManuellOppgaveForOpprettForbehandling(sakId1, 2024)

        verify {
            ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sakId1,
                2024,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )
            ctx.oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId1,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Etteroppgjøret for 2024 er klart til behandling",
                gjelderAar = 2024,
            )
        }
    }

    @Test
    fun `skal tilbakestille etteroppgjoer og opprette ny oppgave`() {
        val ctx = TestContext()

        ctx.returnsForbehandlinger(
            listOf(
                EtteroppgjoerForbehandling.opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ),
            ),
        )
        justRun {
            ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sakId1,
                2024,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                EtteroppgjoerHendelser.TILBAKESTILT,
            )
        }

        ctx.service.tilbakestillEtteroppgjoerOgOpprettForbehandlingsoppgave(sakId1, 2024)

        verify {
            ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sakId1,
                2024,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                EtteroppgjoerHendelser.TILBAKESTILT,
            )
            ctx.oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId1,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = "Etteroppgjøret for 2024 er klart til behandling",
                gjelderAar = 2024,
            )
        }
    }

    @Test
    fun `skal ikke avbryte forbehandling hvis den er ferdigstilt`() {
        val ctx = TestContext()
        val brukerTokenInfo = simpleSaksbehandler()
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ).copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        ctx.returnsForbehandling(forbehandling)
        ctx.returnsOppgaverForReferanse(
            listOf(
                oppgaveForEtteroppgjoer(
                    referanse = forbehandling.id.toString(),
                    saksbehandler = OppgaveSaksbehandler(brukerTokenInfo.ident),
                ),
            ),
        )

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                ctx.service.avbrytForbehandling(
                    forbehandling.id,
                    brukerTokenInfo,
                    AarsakTilAvbryteForbehandling.ANNET,
                    "kommentar",
                )
            }

        assertEquals("FEIL_STATUS_FORBEHANDLING", exception.code)
    }

    @Test
    fun `skal ikke avbryte forbehandling som er revurdering`() {
        val ctx = TestContext()
        val brukerTokenInfo = simpleSaksbehandler()
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ).copy(kopiertFra = UUID.randomUUID())

        ctx.returnsForbehandling(forbehandling)
        ctx.returnsOppgaverForReferanse(
            listOf(
                oppgaveForEtteroppgjoer(
                    referanse = forbehandling.id.toString(),
                    saksbehandler = OppgaveSaksbehandler(brukerTokenInfo.ident),
                ),
            ),
        )

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                ctx.service.avbrytForbehandling(
                    forbehandling.id,
                    brukerTokenInfo,
                    AarsakTilAvbryteForbehandling.ANNET,
                    "kommentar",
                )
            }

        assertEquals("FORBEHANDLING_ER_TILKNYTT_REVURDERING", exception.code)
    }

    @Test
    fun `skal ikke avbryte forbehandling uten begrunnelse`() {
        val ctx = TestContext()
        val brukerTokenInfo = simpleSaksbehandler()
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = ctx.behandling.sak,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                sisteIverksatteBehandling = ctx.behandling.id,
                mottattSkatteoppgjoer = true,
            )

        ctx.returnsForbehandling(forbehandling)
        ctx.returnsOppgaverForReferanse(
            listOf(
                oppgaveForEtteroppgjoer(
                    referanse = forbehandling.id.toString(),
                    saksbehandler = OppgaveSaksbehandler(brukerTokenInfo.ident),
                ),
            ),
        )

        val exception =
            assertThrows(UgyldigForespoerselException::class.java) {
                ctx.service.avbrytForbehandling(
                    forbehandling.id,
                    brukerTokenInfo,
                    AarsakTilAvbryteForbehandling.ANNET,
                    null,
                )
            }

        assertEquals("VERDI_ER_NULL", exception.code)
    }

    @Test
    fun `skal avbryte forbehandling og oppdatere status og oppgaver`() {
        val ctx = TestContext()
        val brukerTokenInfo = simpleSaksbehandler()
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = ctx.behandling.sak,
                innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                sisteIverksatteBehandling = ctx.behandling.id,
                mottattSkatteoppgjoer = true,
            )

        ctx.returnsForbehandling(forbehandling)
        ctx.returnsOppgaverForReferanse(
            listOf(
                oppgaveForEtteroppgjoer(
                    referanse = forbehandling.id.toString(),
                    saksbehandler = OppgaveSaksbehandler(brukerTokenInfo.ident),
                ),
            ),
        )
        justRun { ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(sakId1, 2024, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER) }
        justRun { ctx.oppgaveService.avbrytAapneOppgaverMedReferanse(forbehandling.id.toString(), any()) }
        justRun { ctx.hendelserService.registrerOgSendHendelse(any(), null, any(), any(), any()) }

        ctx.service.avbrytForbehandling(
            forbehandling.id,
            brukerTokenInfo,
            AarsakTilAvbryteForbehandling.ANNET,
            "manuell kommentar",
        )

        verify {
            ctx.dao.lagreForbehandling(
                match {
                    it.id == forbehandling.id &&
                        it.status == EtteroppgjoerForbehandlingStatus.AVBRUTT &&
                        it.aarsakTilAvbrytelse == AarsakTilAvbryteForbehandling.ANNET &&
                        it.aarsakTilAvbrytelseBeskrivelse == "manuell kommentar"
                },
            )
            ctx.etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sakId1,
                2024,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )
            ctx.oppgaveService.avbrytAapneOppgaverMedReferanse(
                forbehandling.id.toString(),
                "Avbrutt manuelt. Årsak: manuell kommentar",
            )
            ctx.hendelserService.registrerOgSendHendelse(any(), null, any(), brukerTokenInfo.ident, null)
        }
    }

    @Test
    fun `skal ikke opprette forbehandling naar det finnes aapne behandlingsoppgaver`() {
        val ctx = TestContext()

        every { ctx.oppgaveService.hentOppgaverForSak(sakId1) } returns
            listOf(
                oppgaveForEtteroppgjoer(
                    type = OppgaveType.FOERSTEGANGSBEHANDLING,
                    kilde = OppgaveKilde.BEHANDLING,
                ),
            )

        val exception =
            assertThrows(IkkeTillattException::class.java) {
                ctx.service.sjekkHarAapneBehandlinger(sakId1, null)
            }

        assertEquals("ALLEREDE_AAPEN_BEHANDLING", exception.code)
    }

    @Test
    fun `skal ikke ferdigstille forbehandling uten brev naar resultatet krever brev`() {
        val ctx = TestContext()
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ).copy(
                    status = EtteroppgjoerForbehandlingStatus.BEREGNET,
                    etteroppgjoerResultatType = no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType.ETTERBETALING,
                )

        ctx.returnsForbehandling(forbehandling)

        val exception =
            assertThrows(UgyldigForespoerselException::class.java) {
                ctx.service.ferdigstillForbehandlingUtenBrev(forbehandling.id, simpleSaksbehandler())
            }

        assertEquals("ETTEROPPGJOER_RESULTAT_TRENGER_BREV", exception.code)
    }

    @Test
    fun `skal ikke lagre ny informasjon fra bruker for ferdigstilte forbehandlinger`() {
        val ctx = TestContext()
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ).copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        ctx.returnsForbehandling(forbehandling)

        assertThrows(ForbehandlingKanIkkeEndres::class.java) {
            ctx.service.lagreInformasjonFraBruker(forbehandling.id, JaNei.JA, JaNei.NEI, null)
        }
    }

    @Test
    fun `skal ikke lagre ny informasjon om opphoer ved doedsfall når forbehandling er ferdigstilt`() {
        val ctx = TestContext()
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ).copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        ctx.returnsForbehandling(forbehandling)

        assertThrows(ForbehandlingKanIkkeEndres::class.java) {
            ctx.service.lagreOmOpphoerSkyldesDoedsfall(forbehandling.id, JaNei.JA, JaNei.JA)
        }
    }

    @Test
    fun `skal ikke lagre informasjon om aktivitetsplikt når forbehandling er ferdigstilt`() {
        val ctx = TestContext()
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = ctx.behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
                    sisteIverksatteBehandling = ctx.behandling.id,
                    mottattSkatteoppgjoer = true,
                ).copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        ctx.returnsForbehandling(forbehandling)

        assertThrows(ForbehandlingKanIkkeEndres::class.java) {
            ctx.service.lagreAktivitetsplikt(forbehandling.id, JaNei.JA, "begrunnelse")
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
                    mottattSkatteoppgjoer = true,
                ).copy(brevId = 123L, varselbrevSendt = LocalDate.now())

        ctx.returnsForbehandling(forbehandling)
        coEvery { ctx.vedtakKlient.hentIverksatteVedtak(sakId1, any()) } returns
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

        val kopiertForbehandling = ctx.service.kopierOgLagreNyForbehandling(uuid, sakId1, null, mockk())

        with(kopiertForbehandling) {
            assertNotEquals(id, forbehandling.id)
            assertEquals(kopiertFra, forbehandling.id)
            assertEquals(sisteIverksatteBehandlingId, ctx.behandling.id)
            assertNull(brevId)
            assertNull(varselbrevSendt)
        }

        verify {
            ctx.dao.lagreForbehandling(kopiertForbehandling)
            ctx.dao.kopierSummerteInntekter(forbehandling.id, kopiertForbehandling.id)
            ctx.dao.kopierPensjonsgivendeInntekt(forbehandling.id, kopiertForbehandling.id)
        }
    }

    private fun oppgaveForEtteroppgjoer(
        referanse: String = UUID.randomUUID().toString(),
        status: Status = Status.UNDER_BEHANDLING,
        type: OppgaveType = OppgaveType.ETTEROPPGJOER,
        kilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        saksbehandler: OppgaveSaksbehandler? = null,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = status,
        enhet = sak().enhet,
        sakId = sakId1,
        kilde = kilde,
        type = type,
        saksbehandler = saksbehandler,
        referanse = referanse,
        gruppeId = null,
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = sak().sakType,
        fnr = sak().ident,
        frist = null,
    )
}
