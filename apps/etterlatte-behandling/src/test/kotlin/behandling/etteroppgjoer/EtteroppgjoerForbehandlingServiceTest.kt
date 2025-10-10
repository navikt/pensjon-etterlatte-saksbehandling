package no.nav.etterlatte.behandling.etteroppgjoer

import io.mockk.Awaits
import io.mockk.Runs
import io.mockk.awaits
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
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
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
        val hendelserService: EtteroppgjoerHendelseService = mockk()
        val sigrunKlient: SigrunKlient = mockk()
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
                hendelserService = hendelserService,
                sigrunKlient = sigrunKlient,
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

        init {
            coEvery {
                behandlingService.hentSisteIverksatteBehandling(sakId1)
            } returns behandling

            every { dao.lagreForbehandling(any()) } returns 1
            every { dao.kopierSummerteInntekter(any(), any()) } returns 1
            every { dao.kopierPensjonsgivendeInntekt(any(), any()) } just runs
        }

        fun returnsForbehandling(forbehandling: EtteroppgjoerForbehandling) {
            coEvery {
                dao.hentForbehandling(any())
            } returns forbehandling
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
