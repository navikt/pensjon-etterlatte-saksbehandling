package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class SkatteoppgjoerHendelserServiceTest {
    private val dao: SkatteoppgjoerHendelserDao = mockk()
    private val sigrunKlient: SigrunKlient = mockk()
    private val etteroppgjoerService: EtteroppgjoerService = mockk()
    private val sakService: SakService = mockk()

    @BeforeEach
    fun setup() {
        Kontekst.set(Context(Self(this::class.java.simpleName), DatabaseContextTest(mockk<DataSource>()), mockk(), null))
    }

    @Test
    fun `skal behandle hendelser fra Sigrun og oppdatere status for relevante etteroppgjoer`() {
        val skatteoppgjoerHendelserService = SkatteoppgjoerHendelserService(dao, sigrunKlient, etteroppgjoerService, sakService)

        val sisteKjoering = HendelserKjoering(10, 10, 0)
        val antall = 10

        coEvery { dao.hentSisteKjoering() } returns sisteKjoering
        coEvery { sigrunKlient.hentHendelsesliste(antall, sisteKjoering.nesteSekvensnummer(), any()) } returns
            HendelseslisteFraSkatt.stub(sisteKjoering.nesteSekvensnummer())

        coEvery { sakService.finnSak(any(), any()) } returns mockk { every { id } returns SakId(1L) }
        coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(SakId(any()), any()) } returns null

        coEvery { sakService.finnSak("5", any()) } returns mockk { every { id } returns SakId(2L) }
        coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(SakId(2L), any()) } returns
            Etteroppgjoer(SakId(2L), 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, false, false, false, false)

        coEvery { dao.lagreKjoering(any()) } returns 1
        coEvery { etteroppgjoerService.oppdaterEtteroppgjoerStatus(any(), any(), any()) } just runs

        runBlocking {
            skatteoppgjoerHendelserService.lesOgBehandleHendelser(HendelseKjoeringRequest(antall))
        }

        coVerify {
            dao.lagreKjoering(
                withArg { kjoering ->
                    kjoering.antallHendelser shouldBe 10
                    kjoering.antallRelevante shouldBe 1
                    kjoering.nesteSekvensnummer().toInt() shouldBe 21
                },
            )
        }

        coVerify {
            etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                any(),
                any(),
                withArg { status ->
                    status shouldBe EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
                },
            )
        }
    }
}
