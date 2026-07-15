package no.nav.etterlatte.prosessering

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.postgres.PostgresTaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.etterlatte.libs.common.behandling.SakType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds

/**
 * Skyggekjøring ende-til-ende i host-en (PoC Fase 4): en task legges i behandlingens
 * `prosessering`-skjema, motoren plukker den og kjører [soeknadMottakSkyggeSteg].
 *
 * - Gyldig søknad → task → FULLFØRT, og steget observerte det simulerte mottaket.
 * - Ugyldig fnr → steget kaster → motoren retry-er og ender i STOPPET (FEIL),
 *   uten at steget noensinne observerer en ugyldig søknad.
 */
@ExtendWith(ProsesseringDatabaseExtension::class)
class SoeknadMottakSkyggeIntegrationTest {
    private fun byggMotor(
        dataSource: DataSource,
        observer: (SoeknadMottakPayload) -> Unit,
    ): Pair<ProcessingEngine, PostgresTaskRepository> {
        val repo = PostgresTaskRepository(dataSource)
        val produsent = StandardTaskProdusent(repo)
        val engine =
            ProcessingEngine(
                repo = repo,
                produsent = produsent,
                steg = listOf(soeknadMottakSkyggeSteg(observer)),
                node = "test-node",
                maxAntallFeil = 3,
                backoff = { 50.milliseconds },
            )
        return engine to repo
    }

    @Test
    fun `gyldig soeknad blir task som fullfoerer og observeres`(dataSource: DataSource) =
        runBlocking {
            val observert = AtomicReference<SoeknadMottakPayload>()
            val (engine, repo) = byggMotor(dataSource) { observert.set(it) }
            val produsent = StandardTaskProdusent(repo)

            val payload =
                SoeknadMottakPayload(
                    soeknadId = "1001",
                    sakType = SakType.BARNEPENSJON,
                    fnrSoeker = "25478323363",
                )
            val taskId = produsent.opprettFrittstående(type = soeknadMottakSkyggeType, payload = payload).verdi

            engine.start()
            withTimeout(30_000) {
                while (repo.finn(taskId)?.status != Status.FULLFØRT) {
                    delay(25)
                }
            }
            engine.stop()

            assertEquals(Status.FULLFØRT, repo.finn(taskId)?.status)
            assertEquals(payload, observert.get(), "Steget skal ha observert det simulerte mottaket")
        }

    @Test
    fun `ugyldig fnr ender i STOPPET uten at mottaket observeres`(dataSource: DataSource) =
        runBlocking {
            val observert = AtomicReference<SoeknadMottakPayload>()
            val (engine, repo) = byggMotor(dataSource) { observert.set(it) }
            val produsent = StandardTaskProdusent(repo)

            val payload =
                SoeknadMottakPayload(
                    soeknadId = "1002",
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    fnrSoeker = "123",
                )
            val taskId = produsent.opprettFrittstående(type = soeknadMottakSkyggeType, payload = payload).verdi

            engine.start()
            withTimeout(30_000) {
                while (repo.finn(taskId)?.status != Status.STOPPET) {
                    delay(25)
                }
            }
            engine.stop()

            val task = repo.finn(taskId)!!
            assertEquals(Status.STOPPET, task.status)
            assertEquals(Stoppaarsak.FEIL, task.stoppaarsak)
            assertTrue(observert.get() == null, "Steget skal aldri observere en ugyldig søknad")
        }
}
