package behandling

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.OppgaveService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.typer.BehandlingsOppgave
import no.nav.etterlatte.typer.OppgaveListe
import no.nav.etterlatte.typer.Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

internal class OppgaveServiceTest {
    @MockK
    lateinit var behandlingKlient: BehandlingKlient

    @InjectMockKs
    lateinit var service: OppgaveService

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this)
    private val accessToken = UUID.randomUUID().toString()

    @Test
    fun hentAlleOppgaver() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingKlient.hentOppgaver(accessToken) } returns OppgaveListe(
            oppgaver = listOf(
                BehandlingsOppgave(
                    behandlingId,
                    BehandlingStatus.VILKAARSVURDERT,
                    OppgaveStatus.NY,
                    Sak("ident", "saktype", 1),
                    ZonedDateTime.now(),
                    LocalDate.now(),
                    BehandlingType.REVURDERING,
                    1
                )
            )
        )

        // TODO skriv noen bedre tester av sammenflettingen av forskjellige oppgaver her
        coEvery { behandlingKlient.hentUhaandterteGrunnlagshendelser(accessToken) } returns listOf()

        val resultat = runBlocking { service.hentAlleOppgaver(accessToken) }

        assertEquals(BehandlingStatus.VILKAARSVURDERT, resultat.oppgaver.first().status)
        assertEquals(BehandlingType.REVURDERING, resultat.oppgaver.first().behandlingType)
        assertEquals(OppgaveStatus.NY, resultat.oppgaver.first().oppgaveStatus)
    }
}