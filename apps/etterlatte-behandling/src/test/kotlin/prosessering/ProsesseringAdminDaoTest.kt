package no.nav.etterlatte.prosessering

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.postgres.PostgresTaskRepository
import no.nav.etterlatte.libs.common.behandling.SakType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import javax.sql.DataSource

/**
 * Lese-/admin-DAO-en ([ProsesseringAdminDao]) som operatør-GUIet leser og styrer tasks
 * gjennom (PoC Fase 4c). Kjøres mot behandlingens ekte `prosessering`-skjema på Testcontainers.
 */
@ExtendWith(ProsesseringDatabaseExtension::class)
class ProsesseringAdminDaoTest {
    private fun payload(id: String) = SoeknadMottakPayload(soeknadId = id, sakType = SakType.BARNEPENSJON, fnrSoeker = "25478323363")

    private fun opprettTask(
        repo: PostgresTaskRepository,
        id: String,
    ): Long = StandardTaskProdusent(repo).opprettFrittstående(type = soeknadMottakSkyggeType, payload = payload(id)).verdi

    private fun tøm(dataSource: DataSource) = dataSource.connection.use { it.createStatement().execute("TRUNCATE prosessering.task") }

    @Test
    fun `list henter tasks og filtrerer paa status`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = ProsesseringAdminDao(dataSource)

        opprettTask(repo, "1")
        val stoppetId = opprettTask(repo, "2")
        repo.markFeilet(id = stoppetId, nyStatus = Status.STOPPET, stoppaarsak = Stoppaarsak.FEIL, nesteTriggerTid = Instant.now())

        assertEquals(2, dao.list(status = null, limit = 100).size)

        val stoppede = dao.list(status = Status.STOPPET, limit = 100)
        assertEquals(1, stoppede.size)
        assertEquals(stoppetId, stoppede.single().id)

        assertEquals(1, dao.list(status = Status.KLAR, limit = 100).size)
    }

    @Test
    fun `finn returnerer en enkelt task`(dataSource: DataSource) {
        val repo = PostgresTaskRepository(dataSource)
        val dao = ProsesseringAdminDao(dataSource)

        val id = opprettTask(repo, "3")

        val task = dao.finn(id)
        assertEquals(id, task?.id)
        assertEquals(Status.KLAR, task?.status)
        assertNull(dao.finn(id + 9999), "Ukjent id skal gi null")
    }

    @Test
    fun `rekjor setter STOPPET tilbake til KLAR og nullstiller antall_feil`(dataSource: DataSource) {
        val repo = PostgresTaskRepository(dataSource)
        val dao = ProsesseringAdminDao(dataSource)

        val id = opprettTask(repo, "4")
        repo.markFeilet(id = id, nyStatus = Status.STOPPET, stoppaarsak = Stoppaarsak.FEIL, nesteTriggerTid = Instant.now())
        assertEquals(1, repo.finn(id)?.antallFeil)

        assertTrue(dao.rekjor(id), "Rekjør av en STOPPET task skal endre en rad")

        val etterpaa = repo.finn(id)!!
        assertEquals(Status.KLAR, etterpaa.status)
        assertEquals(0, etterpaa.antallFeil, "antall_feil skal nullstilles så motoren får fulle retries")
        assertNull(etterpaa.stoppaarsak)
    }

    @Test
    fun `rekjor gjoer ingenting for en task som ikke er STOPPET eller AVBRUTT`(dataSource: DataSource) {
        val repo = PostgresTaskRepository(dataSource)
        val dao = ProsesseringAdminDao(dataSource)

        val id = opprettTask(repo, "5")

        assertFalse(dao.rekjor(id), "KLAR task skal ikke kunne rekjøres")
        assertEquals(Status.KLAR, repo.finn(id)?.status)
    }
}
