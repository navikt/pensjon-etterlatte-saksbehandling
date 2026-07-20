package no.nav.etterlatte.prosessering

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.postgres.PostgresTaskRepository
import no.nav.etterlatte.libs.common.behandling.SakType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import javax.sql.DataSource

/**
 * Idempotens-sjekken ([SoeknadSkyggeDao]) som skygge-ruten deduper på (PoC Fase 4d).
 * Kjøres mot behandlingens ekte `prosessering`-skjema på Testcontainers.
 *
 * En søknad skal gi nøyaktig én task: finnes det allerede en task for `soeknadId` i en
 * hvilken som helst status unntatt AVBRUTT, blokkeres ny innkøing. Bare AVBRUTT (operatør
 * har avfeid tasken) åpner for ny innkøing.
 */
@ExtendWith(ProsesseringDatabaseExtension::class)
class SoeknadSkyggeDaoTest {
    private fun payload(id: String) = SoeknadMottakPayload(soeknadId = id, sakType = SakType.BARNEPENSJON, fnrSoeker = "25478323363")

    private fun opprettTask(
        repo: PostgresTaskRepository,
        soeknadId: String,
    ): Long = StandardTaskProdusent(repo).opprettFrittstående(type = soeknadMottakSkyggeType, payload = payload(soeknadId)).verdi

    private fun tøm(dataSource: DataSource) = dataSource.connection.use { it.createStatement().execute("TRUNCATE prosessering.task") }

    @Test
    fun `ingen task for soeknaden gir false`(dataSource: DataSource) {
        tøm(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        assertFalse(dao.harAlleredeHaandtertSoeknad("ukjent"))
    }

    @Test
    fun `KLAR task for soeknaden gir true, annen soeknadId gir false`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        opprettTask(repo, "12345")

        assertTrue(dao.harAlleredeHaandtertSoeknad("12345"), "En KLAR task for søknaden teller som håndtert")
        assertFalse(dao.harAlleredeHaandtertSoeknad("99999"), "Annen soeknadId skal ikke matche")
    }

    @Test
    fun `KJOERER task for soeknaden gir true`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        opprettTask(repo, "22222")
        val plukket = repo.claimBatch(limit = 10)
        assertTrue(plukket.any { it.status == Status.KJØRER }, "claim skal sette task til KJØRER")

        assertTrue(dao.harAlleredeHaandtertSoeknad("22222"), "En KJØRER task for søknaden teller som håndtert")
    }

    @Test
    fun `FULLFOERT task for soeknaden gir true`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        val id = opprettTask(repo, "33333")
        repo.iEgenTransaksjon { tx -> repo.markerFullført(transaksjon = tx, id = id) }

        assertTrue(
            dao.harAlleredeHaandtertSoeknad("33333"),
            "En FULLFØRT task skal blokkere ny innkøing — søknaden er ferdig håndtert",
        )
    }

    @Test
    fun `STOPPET task for soeknaden gir true`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        val id = opprettTask(repo, "44444")
        repo.markFeilet(id = id, nyStatus = Status.STOPPET, stoppaarsak = Stoppaarsak.FEIL, nesteTriggerTid = Instant.now())

        assertTrue(
            dao.harAlleredeHaandtertSoeknad("44444"),
            "En STOPPET task følges opp via rekjør, ikke ved ny innkøing",
        )
    }

    @Test
    fun `AVBRUTT task for soeknaden gir false`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        val id = opprettTask(repo, "55555")
        settStatusAvbrutt(dataSource, id)

        assertFalse(
            dao.harAlleredeHaandtertSoeknad("55555"),
            "En AVBRUTT task er avfeid av operatør — søknaden kan køes på nytt",
        )
    }

    private fun settStatusAvbrutt(
        dataSource: DataSource,
        id: Long,
    ) = dataSource.connection.use { connection ->
        connection.prepareStatement("UPDATE prosessering.task SET status = 'AVBRUTT' WHERE id = ?").use { statement ->
            statement.setLong(1, id)
            statement.executeUpdate()
        }
    }
}
