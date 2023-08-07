package no.nav.etterlatte.oppgave

import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GosysOppgaveServiceImplTest {
    private val gosysOppgaveKlient = mockk<GosysOppgaveKlient>()
    private val pdlKlient = mockk<PdlKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val brukerTokenInfo = mockk<BrukerTokenInfo>()

    private val service = GosysOppgaveServiceImpl(gosysOppgaveKlient, pdlKlient, featureToggleService)

    @Test
    fun `skal hente oppgaver og deretter folkeregisterIdent for unike identer`() {
        every { featureToggleService.isEnabled(any(), false) } returns true

        coEvery { gosysOppgaveKlient.hentOppgaver(any(), any(), brukerTokenInfo) } returns GosysOppgaver(
            antallTreffTotalt = 3,
            oppgaver = listOf(
                GosysOppgave(
                    id = 1,
                    tema = "EYB",
                    behandlingstema = "",
                    oppgavetype = "",
                    opprettetTidspunkt = Tidspunkt.now(),
                    tildeltEnhetsnr = "4808",
                    tilordnetRessurs = null,
                    aktoerId = "53771238272763",
                    beskrivelse = "Beskrivelse av oppgaven",
                    status = "NY",
                    fristFerdigstillelse = LocalDate.now().plusDays(7)
                ),
                GosysOppgave(
                    id = 2,
                    tema = "EYB",
                    behandlingstema = "",
                    oppgavetype = "",
                    opprettetTidspunkt = Tidspunkt.now().minus(5L, ChronoUnit.DAYS),
                    tildeltEnhetsnr = "4808",
                    tilordnetRessurs = "A123456",
                    aktoerId = "53771238272763",
                    beskrivelse = "Beskrivelse av oppgave med id 2",
                    status = "TIL_ATTESTERING",
                    fristFerdigstillelse = LocalDate.now().plusDays(14)
                ),
                GosysOppgave(
                    id = 3,
                    tema = "EYO",
                    behandlingstema = "",
                    oppgavetype = "",
                    opprettetTidspunkt = Tidspunkt.now().minus(3L, ChronoUnit.DAYS),
                    tildeltEnhetsnr = "4808",
                    tilordnetRessurs = null,
                    aktoerId = "78324720383742",
                    beskrivelse = "Omstillingsstønad oppgavebeskrivelse",
                    status = "NY",
                    fristFerdigstillelse = LocalDate.now().plusDays(4)
                )
            )
        )
        every { pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(setOf("53771238272763", "78324720383742")) } returns
            mapOf(
                "53771238272763" to "01010812345",
                "78324720383742" to "29048012345"
            )

        val resultat = runBlocking {
            service.hentOppgaver(brukerTokenInfo)
        }

        resultat shouldHaveSize 3
        resultat.filter { it.fnr == "01010812345" } shouldHaveSize 2
        resultat.filter { it.fnr == "29048012345" } shouldHaveSize 1
    }
}