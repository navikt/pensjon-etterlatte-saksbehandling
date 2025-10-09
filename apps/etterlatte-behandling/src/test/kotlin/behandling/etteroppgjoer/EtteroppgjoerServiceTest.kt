package no.nav.etterlatte.behandling.etteroppgjoer

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EtteroppgjoerServiceTest {
    private val dao: EtteroppgjoerDao = mockk()
    private val sigrunKlient: SigrunKlient = mockk()
    private val beregningKlient: BeregningKlient = mockk()
    private val behandlingService: BehandlingService = mockk()

    private val etteroppgjoerService: EtteroppgjoerService = EtteroppgjoerService(
        dao = dao,
        vedtakKlient = mockk(),
        behandlingService = behandlingService,
        beregningKlient = beregningKlient,
        sigrunKlient = sigrunKlient,
    )

    val sakId = sakId1

    @BeforeEach
    fun setup() {
        every {
            dao.hentEtteroppgjoerForInntektsaar(sakId, any())
        } returns mockk {
            every { status } returns EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
        }

        coEvery {
            dao.lagreEtteroppgjoer(any())
        } returns 1
    }

    @Test
    fun `skal opprette etteroppgjør med status MOTTATT_SKATTEOPPGJOER hvis vi får svar fra sigrun`() {
        val foerstegangsBehandling = foerstegangsbehandling(sakId = sakId,
            sakType = SakType.OMSTILLINGSSTOENAD,
            status = BehandlingStatus.ATTESTERT,
            virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato= YearMonth.now().minusYears(1))
        )

        mockOpprettEtteroppgjoer()

        coEvery {
            sigrunKlient.hentPensjonsgivendeInntekt(any(), any())} returns PensjonsgivendeInntektFraSkatt.stub()



        val etteroppgjoer = runBlocking {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                sistIverksatteBehandling = foerstegangsBehandling,
                inntektsaar = 2023)
        }

        assertEquals(etteroppgjoer!!.status, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER)
    }

    @Test
    fun `skal opprette etteroppgjør med status VENTER_PAA_SKATTEOPPGJOER hvis vi ikke får svar fra sigrun`() {
        val sakId = sakId1
        val foerstegangsBehandling = foerstegangsbehandling(sakId = sakId,
            sakType = SakType.OMSTILLINGSSTOENAD,
            status = BehandlingStatus.ATTESTERT,
            virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato= YearMonth.now().minusYears(1))
        )

        mockOpprettEtteroppgjoer()



        coEvery {
            sigrunKlient.hentPensjonsgivendeInntekt(any(), any())} throws Exception("Ingen skatteoppgjør funnet :/")

        val etteroppgjoer = runBlocking {
            etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                sistIverksatteBehandling = foerstegangsBehandling,
                inntektsaar = 2023)
        }

        assertEquals(etteroppgjoer!!.status, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER)
    }

    private fun mockOpprettEtteroppgjoer() {
        coEvery {
            beregningKlient.hentSanksjoner(any(), any())
        } returns emptyList()

        coEvery {
            beregningKlient.hentBeregningsgrunnlag(any(), any())
        } returns mockk {
            every { institusjonsopphold } returns emptyList()
        }

        coEvery {
            beregningKlient.hentOverstyrtBeregning(any(), any())
        } returns mockk()

        every {
            behandlingService.hentFoersteDoedsdato(any(), any())
        } returns null
    }
}