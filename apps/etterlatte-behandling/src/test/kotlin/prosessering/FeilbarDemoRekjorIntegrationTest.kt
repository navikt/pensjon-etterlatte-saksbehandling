package no.nav.etterlatte.prosessering

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.postgres.PostgresTaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds

/**
 * Beviser rekjøring-styrken i prosessering-konseptet ende-til-ende (PoC Fase 4d):
 * en task som feiler på en **forbigående** feil ender i STOPPET, og en manuell
 * **rekjør** (samme handling operatøren gjør i GUIet, via [ProsesseringAdminDao.rekjor])
 * tar den helt til FULLFØRT når den simulerte avhengigheten er «oppe» igjen.
 *
 * Dette er nettopp det rapids-and-rivers ikke gir alene: et stoppet arbeid som kan
 * plukkes opp igjen og fullføres.
 */
@ExtendWith(ProsesseringDatabaseExtension::class)
class FeilbarDemoRekjorIntegrationTest {
    private fun byggMotor(
        dataSource: DataSource,
        observer: (FeilbarDemoPayload) -> Unit,
    ): Pair<ProcessingEngine, PostgresTaskRepository> {
        val repo = PostgresTaskRepository(dataSource)
        val produsent = StandardTaskProdusent(repo)
        val engine =
            ProcessingEngine(
                repo = repo,
                produsent = produsent,
                steg = listOf(feilbarDemoSteg(observer)),
                node = "test-node",
                maxAntallFeil = 3,
                backoff = { 50.milliseconds },
            )
        return engine to repo
    }

    @Test
    fun `task som feiler ender i STOPPET og fullfoerer ved rekjoering`(dataSource: DataSource) =
        runBlocking {
            val observert = AtomicReference<FeilbarDemoPayload>()
            val (engine, repo) = byggMotor(dataSource) { observert.set(it) }
            val produsent = StandardTaskProdusent(repo)
            val admin = ProsesseringAdminDao(dataSource)

            val simulertOppeFra = Instant.now().plusMillis(1500)
            val payload = FeilbarDemoPayload(demoId = "demo-1", simulertOppeFra = simulertOppeFra)
            val taskId = produsent.opprettFrittstående(type = feilbarDemoType, payload = payload).verdi

            engine.start()

            withTimeout(30_000) {
                while (repo.finn(taskId)?.status != Status.STOPPET) {
                    delay(25)
                }
            }
            val stoppet = repo.finn(taskId)!!
            assertEquals(Status.STOPPET, stoppet.status)
            assertEquals(Stoppaarsak.FEIL, stoppet.stoppaarsak)
            assertNull(observert.get(), "Steget skal aldri observere en fullføring mens avhengigheten er «nede»")

            while (Instant.now().isBefore(simulertOppeFra)) {
                delay(25)
            }
            assertTrue(admin.rekjor(taskId), "Rekjør skal sette en STOPPET task tilbake til KLAR")

            withTimeout(30_000) {
                while (repo.finn(taskId)?.status != Status.FULLFØRT) {
                    delay(25)
                }
            }
            engine.stop()

            assertEquals(Status.FULLFØRT, repo.finn(taskId)?.status)
            assertEquals(payload, observert.get(), "Steget skal ha fullført etter rekjøring")
        }
}
