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
 * En uferdig (KLAR/KJØRER) task for samme `soeknadId` skal blokkere ny innkøing;
 * FULLFØRT/STOPPET skal ikke det.
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

        assertFalse(dao.finnesUferdigTaskForSoeknad("ukjent"))
    }

    @Test
    fun `KLAR task for soeknaden gir true, annen soeknadId gir false`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        opprettTask(repo, "12345")

        assertTrue(dao.finnesUferdigTaskForSoeknad("12345"), "En KLAR task for søknaden er uferdig")
        assertFalse(dao.finnesUferdigTaskForSoeknad("99999"), "Annen soeknadId skal ikke matche")
    }

    @Test
    fun `KJOERER task for soeknaden gir true`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        opprettTask(repo, "22222")
        val plukket = repo.claimBatch(limit = 10)
        assertTrue(plukket.any { it.status == Status.KJØRER }, "claim skal sette task til KJØRER")

        assertTrue(dao.finnesUferdigTaskForSoeknad("22222"), "En KJØRER task for søknaden er uferdig")
    }

    @Test
    fun `FULLFOERT task for soeknaden gir false`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        val id = opprettTask(repo, "33333")
        repo.iEgenTransaksjon { tx -> repo.markerFullført(transaksjon = tx, id = id) }

        assertFalse(dao.finnesUferdigTaskForSoeknad("33333"), "En FULLFØRT task skal ikke blokkere ny innkøing")
    }

    @Test
    fun `STOPPET task for soeknaden gir false`(dataSource: DataSource) {
        tøm(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val dao = SoeknadSkyggeDao(dataSource)

        val id = opprettTask(repo, "44444")
        repo.markFeilet(id = id, nyStatus = Status.STOPPET, stoppaarsak = Stoppaarsak.FEIL, nesteTriggerTid = Instant.now())

        assertFalse(dao.finnesUferdigTaskForSoeknad("44444"), "En STOPPET task venter på operatør, ikke ny innkøing")
    }
}
