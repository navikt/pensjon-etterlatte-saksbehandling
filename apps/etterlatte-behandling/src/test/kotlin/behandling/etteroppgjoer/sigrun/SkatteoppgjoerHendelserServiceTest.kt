package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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
import no.nav.etterlatte.behandling.etteroppgjoer.SkalHaEtteroppgjoerResultat
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class SkatteoppgjoerHendelserServiceTest {
    private val dao: SkatteoppgjoerHendelserDao = mockk()
    private val sigrunKlient: SigrunKlient = mockk()
    private val etteroppgjoerService: EtteroppgjoerService = mockk()

    @BeforeEach
    fun setup() {
        Kontekst.set(Context(Self(this::class.java.simpleName), DatabaseContextTest(mockk<DataSource>()), mockk(), null))
    }

    @Test
    fun `skal behandle hendelser fra Sigrun og oppdatere status for relevante etteroppgjoer`() {
        val skatteoppgjoerHendelserService = SkatteoppgjoerHendelserService(dao, sigrunKlient, etteroppgjoerService)

        val sisteKjoering = HendelserKjoering(10, 10, 0)
        val antall = 10

        coEvery { dao.hentSisteKjoering() } returns sisteKjoering
        coEvery { sigrunKlient.hentHendelsesliste(antall, sisteKjoering.nesteSekvensnummer(), any()) } returns
            HendelseslisteFraSkatt.stub(sisteKjoering.nesteSekvensnummer())
        coEvery { etteroppgjoerService.skalHaEtteroppgjoer(any(), any()) } returns SkalHaEtteroppgjoerResultat(false, mockk())
        coEvery { etteroppgjoerService.skalHaEtteroppgjoer("5", any()) } returns
            SkalHaEtteroppgjoerResultat(
                true,
                Etteroppgjoer(
                    SakId(10),
                    2024,
                    mockk(),
                ),
            )

        coEvery { dao.lagreKjoering(any()) } returns 1
        coEvery { etteroppgjoerService.oppdaterEtteroppgjoerStatus(any(), any(), any()) } just runs

        runBlocking {
            skatteoppgjoerHendelserService.startHendelsesKjoering(HendelseKjoeringRequest(antall))
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
