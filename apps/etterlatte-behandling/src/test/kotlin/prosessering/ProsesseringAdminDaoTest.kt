package no.nav.etterlatte.prosessering

import no.nav.etterlatte.DatabaseExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

/** Forbereder på TaskLoggRepository fra navikt/efterlatte-prosessering#25 (se TaskHendelse.kt). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class ProsesseringAdminDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var prosesseringAdminDao: ProsesseringAdminDao

    @BeforeAll
    fun beforeAll() {
        prosesseringAdminDao = ProsesseringAdminDao(dataSource)
    }

    private fun opprettTask(): Long =
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO public.prosessering_task (type, status, payload, trigger_tid)
                    VALUES ('test', 'KLAR', '{}', ?)
                    RETURNING id
                    """.trimIndent(),
                ).use { statement ->
                    statement.setTimestamp(1, Timestamp.from(Instant.now()))
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getLong("id")
                    }
                }
        }

    @Test
    fun `leggTilHendelse skriver kommentar og avvik og hentHendelser gir dem tilbake eldst foerst`() {
        val taskId = opprettTask()

        prosesseringAdminDao.leggTilHendelse(
            taskId = taskId,
            type = TaskHendelseType.KOMMENTAR,
            melding = "Undersøker en treg respons fra PDL",
            endretAv = "Z123456",
            node = PROSESSERING_NODE,
        )
        val avvik =
            prosesseringAdminDao.leggTilHendelse(
                taskId = taskId,
                type = TaskHendelseType.AVVIK,
                melding = "PdlKlient svarte 503 tre ganger på rad",
                endretAv = "Z123456",
                node = PROSESSERING_NODE,
            )

        assertEquals(TaskHendelseType.AVVIK, avvik.type)
        assertEquals(taskId, avvik.taskId)
        assertEquals(PROSESSERING_NODE, avvik.node)

        val hendelser = prosesseringAdminDao.hentHendelser(taskId)
        assertEquals(2, hendelser.size)
        assertEquals(TaskHendelseType.KOMMENTAR, hendelser[0].type)
        assertEquals(TaskHendelseType.AVVIK, hendelser[1].type)
    }

    @Test
    fun `hentHendelser for task uten hendelser gir tom liste`() {
        val taskId = opprettTask()

        assertEquals(emptyList<TaskHendelse>(), prosesseringAdminDao.hentHendelser(taskId))
    }

    @Test
    fun `leggTilHendelse mot ukjent task kaster TaskIkkeFunnet`() {
        assertThrows(TaskIkkeFunnet::class.java) {
            prosesseringAdminDao.leggTilHendelse(
                taskId = -1,
                type = TaskHendelseType.KOMMENTAR,
                melding = "skal ikke lagres",
                endretAv = "Z123456",
                node = PROSESSERING_NODE,
            )
        }
    }

    @Test
    fun `leggTilHendelse med STATUS_ENDRET er ikke lov for admin-API-et`() {
        val taskId = opprettTask()

        assertThrows(IllegalArgumentException::class.java) {
            prosesseringAdminDao.leggTilHendelse(
                taskId = taskId,
                type = TaskHendelseType.STATUS_ENDRET,
                melding = "skal ikke skrives herfra",
                endretAv = "system",
                node = PROSESSERING_NODE,
            )
        }
        assertNull(prosesseringAdminDao.hentHendelser(taskId).firstOrNull())
    }
}
