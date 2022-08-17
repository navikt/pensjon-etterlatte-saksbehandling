package behandling

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.typer.Sak
import no.nav.etterlatte.typer.Saker
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
    @Disabled
    fun hentAlleOppgaver() {
        coEvery { behandlingKlient.hentSaker(accessToken) } returns Saker(listOf(1, 4, 8).map {
            Sak("",
                "",
                it.toLong())
        })
        coEvery { behandlingKlient.hentBehandlingerForSak(1, accessToken) } returns BehandlingListe(listOf(
            BehandlingSammendrag(UUID.randomUUID(), 1, null, null, null, null, null)
        ))
        coEvery { behandlingKlient.hentBehandlingerForSak(4, accessToken) } returns BehandlingListe(emptyList())
        coEvery { behandlingKlient.hentBehandlingerForSak(8, accessToken) } returns BehandlingListe(listOf(
            BehandlingSammendrag(UUID.randomUUID(), 8, null, null, null, null, null),
            BehandlingSammendrag(UUID.randomUUID(), 8, null, null, null, null, null)
        ))
        val resultat = runBlocking { service.hentAlleOppgaver(accessToken) }

    }
}
