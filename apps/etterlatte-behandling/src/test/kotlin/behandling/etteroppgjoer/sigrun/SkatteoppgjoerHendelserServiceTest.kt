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
import no.nav.etterlatte.behandling.etteroppgjoer.SkatteoppgjoerHendelse
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

class SkatteoppgjoerHendelserServiceTest {
    private val dao: SkatteoppgjoerHendelserDao = mockk()
    private val sigrunKlient: SigrunKlient = mockk()
    private val etteroppgjoerService: EtteroppgjoerService = mockk()
    private val sakService: SakService = mockk()
    private val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService = mockk()

    @BeforeEach
    fun setup() {
        Kontekst.set(Context(Self(this::class.java.simpleName), DatabaseContextTest(mockk<DataSource>()), mockk(), null))
    }

    @Test
    fun `skal behandle hendelser fra Sigrun og oppdatere status for relevante etteroppgjoer`() {
        val skatteoppgjoerHendelserService =
            SkatteoppgjoerHendelserService(dao, sigrunKlient, etteroppgjoerService, sakService)

        val sisteSekvensnummer = 10.toLong()
        val sisteKjoering = HendelserKjoering(sisteSekvensnummer, 10, 0, Tidspunkt.now())
        val antall = 10
        val harOms1 = "123"
        val harOms2 = "456"

        coEvery { dao.hentSisteKjoering() } returns sisteKjoering
        val registreringstidspunktSisteHendelse = Tidspunkt.now().minus(5, ChronoUnit.HOURS)
        coEvery { sigrunKlient.hentHendelsesliste(antall, sisteSekvensnummer + 1, any()) } returns
            HendelseslisteFraSkatt(
                listOf(
                    hendelse("789", 2024, sisteSekvensnummer + 1),
                    hendelse("963", 2024, sisteSekvensnummer + 2),
                    hendelse(harOms1, 2024, sisteSekvensnummer + 3),
                    hendelse(harOms1, 2025, sisteSekvensnummer + 4),
                    hendelse(harOms2, 2024, sisteSekvensnummer + 5, registreringstidspunktSisteHendelse),
                ),
            )

        coEvery { sakService.finnSak(any(), any()) } returns mockk { every { id } returns SakId(1L) }
        coEvery { sakService.finnSak(harOms1, any<SakType>()) } returns mockk { every { id } returns SakId(2L) }
        coEvery { sakService.finnSak(harOms2, any<SakType>()) } returns mockk { every { id } returns SakId(3L) }

        coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(SakId(2L), 2024) } returns
            Etteroppgjoer(SakId(2L), 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, false, false, false, false)
        coEvery { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(SakId(3L), any()) } returns
            Etteroppgjoer(SakId(3L), 2024, EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, false, false, false, false)

        coEvery { dao.lagreKjoering(any()) } returns 1
        coEvery { etteroppgjoerService.haandterSkatteoppgjoerMottatt(any(), any(), any()) } just runs

        coEvery {
            etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(any(), any(), any())
        } just runs

        runBlocking {
            skatteoppgjoerHendelserService.lesOgBehandleHendelser(HendelseKjoeringRequest(antall))
        }

        coVerify {
            dao.lagreKjoering(
                withArg { kjoering ->
                    kjoering.antallHendelser shouldBe 5
                    kjoering.antallRelevante shouldBe 2
                    kjoering.nesteSekvensnummer().toInt() shouldBe 16
                    kjoering.sisteRegistreringstidspunkt shouldBe registreringstidspunktSisteHendelse
                },
            )
        }

        coVerify {
            etteroppgjoerService.haandterSkatteoppgjoerMottatt(
                any(),
                any(),
                any(),
            )
        }
    }

    private fun hendelse(
        ident: String,
        periode: Int,
        sekvensnummer: Long,
        registreringstidspunkt: Tidspunkt? = Tidspunkt.now(),
    ): SkatteoppgjoerHendelse =
        SkatteoppgjoerHendelse(
            periode.toString(),
            SigrunKlient.HENDELSETYPE_NY,
            ident,
            sekvensnummer,
            registreringstidspunkt,
        )
}
