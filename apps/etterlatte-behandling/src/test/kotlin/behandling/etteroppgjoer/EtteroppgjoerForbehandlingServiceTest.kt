package no.nav.etterlatte.behandling.etteroppgjoer

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.oppgave.OppgaveService
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
        val hendelserService: EtteroppgjoerHendelseService = mockk()
        val beregningKlient: BeregningKlient = mockk()
        val behandlingService: BehandlingService = mockk()
        val vedtakKlient: VedtakKlient = mockk()
        val etteroppgjoerTempService: EtteroppgjoerTempService = mockk()

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
                etteroppgjoerTempService = etteroppgjoerTempService,
            )

        val behandling =
            foerstegangsbehandling(
                sakId = sakId1,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
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
            coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any()) } returns etteroppgjoer

            every { dao.lagreForbehandling(any()) } returns 1
            every { dao.kopierSummerteInntekter(any(), any()) } returns 1
            every { dao.kopierPensjonsgivendeInntekt(any(), any()) } just runs
        }

        fun returnsForbehandling(forbehandling: EtteroppgjoerForbehandling) {
            coEvery {
                dao.hentForbehandling(any())
            } returns forbehandling
        }

        fun returnsForbehandlinger(forbehandlinger: List<EtteroppgjoerForbehandling>) {
            coEvery {
                dao.hentForbehandlinger(any())
            } returns forbehandlinger
        }

        fun returnsOppgave(oppgave: OppgaveIntern) {
            coEvery { oppgaveService.hentOppgave(any()) } returns oppgave
        }

        fun returnsSak(sak: Sak) {
            coEvery { sakDao.hentSak(any()) } returns sak
        }

        fun returnsEtteroppgjoer(etteroppgjoer: Etteroppgjoer?) {
            coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(any(), any()) } returns etteroppgjoer
        }
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

    @ParameterizedTest(name = "skal ikke opprette forbehandling for status={0}")
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["MOTTATT_SKATTEOPPGJOER"],
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

        ctx.returnsEtteroppgjoer(null)

        val exception =
            assertThrows(IkkeTillattException::class.java) {
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
                ).copy(brevId = 123L, varselbrevSendt = LocalDate.now())

        ctx.returnsForbehandling(forbehandling)

        val kopiertForbehandling = ctx.service.kopierOgLagreNyForbehandling(uuid, sakId1)

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
}
