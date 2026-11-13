package no.nav.etterlatte.modules.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.lagContext
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangServiceSjekker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID.randomUUID

class BeregningTilgangServiceTest {
    private val tilgangService = mockk<TilgangServiceSjekker>()
    private val sakTilgangDao = mockk<SakTilgangDao>()
    private val saksbehandler = mockSaksbehandler()
    private val bruker = simpleSaksbehandler()
    private val behandlingId = randomUUID()
    private val service = BeregningTilgangService(tilgangService)

    @BeforeEach
    fun beforeEach() {
        Kontekst.set(lagContext(saksbehandler, sakTilgangDao = sakTilgangDao))
    }

    @AfterEach
    fun afterEach() {
        Kontekst.remove()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `lesetilgang skal sjekke skjerming uten aa kreve skrivetilgang`(harTilgang: Boolean) =
        runBlocking<Unit> {
            every { saksbehandler.hentEnheterMedSkrivetilgang() } returns emptyList()
            every { tilgangService.harTilgangTilBehandling(behandlingId, saksbehandler.saksbehandlerMedRoller) } returns harTilgang

            service.harTilgangTilBehandling(behandlingId, false, bruker) shouldBe harTilgang

            verify(exactly = 0) { sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(any()) }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skrivetilgang skal ogsaa sjekke skjerming`(harTilgang: Boolean) =
        runBlocking<Unit> {
            every { sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(behandlingId) } returns
                SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)
            every { tilgangService.harTilgangTilBehandling(behandlingId, saksbehandler.saksbehandlerMedRoller) } returns harTilgang

            service.harTilgangTilBehandling(behandlingId, true, bruker) shouldBe harTilgang
        }

    @Test
    fun `skal avvise skriving uten rolle eller enhet med skrivetilgang`() =
        runBlocking<Unit> {
            every { saksbehandler.hentEnheterMedSkrivetilgang() } returns emptyList()
            every { sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(behandlingId) } returns
                SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)

            service.harTilgangTilBehandling(behandlingId, true, bruker) shouldBe false

            verify(exactly = 0) { tilgangService.harTilgangTilBehandling(any(), any()) }
        }

    @Test
    fun `skal avvise skriving naar saken mangler`() =
        runBlocking<Unit> {
            every { sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(behandlingId) } returns null

            service.harTilgangTilBehandling(behandlingId, true, bruker) shouldBe false

            verify(exactly = 0) { tilgangService.harTilgangTilBehandling(any(), any()) }
        }

    @Test
    fun `skal ikke skjule tekniske feil i tilgangssjekken`() =
        runBlocking<Unit> {
            val feil = IllegalStateException("Databasen er utilgjengelig")
            every { tilgangService.harTilgangTilBehandling(any(), any()) } throws feil

            assertThrows<IllegalStateException> {
                service.harTilgangTilBehandling(behandlingId, false, bruker)
            } shouldBeSameInstanceAs feil
        }
}
