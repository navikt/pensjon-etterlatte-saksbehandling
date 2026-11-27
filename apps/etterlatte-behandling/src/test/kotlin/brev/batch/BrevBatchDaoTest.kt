package no.nav.etterlatte.brev.batch

import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.brev.BrevBatchSakStatus
import no.nav.etterlatte.brev.BrevBatchStatus
import no.nav.etterlatte.brev.BrevBatchSteg
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class BrevBatchDaoTest(
    private val ds: DataSource,
) {
    private val dao = BrevBatchDao(ds)

    @BeforeEach
    fun rydd() {
        ds.connection.use { it.createStatement().use { stmt -> stmt.execute("TRUNCATE brev_batch CASCADE") } }
    }

    private fun opprett(
        noekkel: UUID = UUID.randomUUID(),
        request: no.nav.etterlatte.brev.BrevBatchRequest = batchRequest(),
    ) = BrevBatchService.normaliser(request).let { dao.opprett("innsender", noekkel, it, BrevBatchService.fingeravtrykk(it)) }

    @Test
    fun `batch og saker opprettes atomisk med idempotens og eierkontroll`() {
        val noekkel = UUID.randomUUID()
        val id = opprett(noekkel, batchRequest(listOf(3, 1, 3, 2)))
        assertEquals(id, opprett(noekkel, batchRequest(listOf(2, 3, 1))))
        assertEquals(3, dao.hent(id, "innsender")!!.antallSaker)
        assertNull(dao.hent(id, "annen"))
        assertNull(dao.hentSaker(id, "annen", 0, 100))
        val side = dao.hentSaker(id, "innsender", 1, 1)!!
        assertEquals(
            2L,
            side.saker
                .single()
                .sakId.sakId,
        )
        assertEquals(3, side.totalt)
        assertEquals(409, assertThrows(ForespoerselException::class.java) { opprett(noekkel, batchRequest(listOf(4))) }.status)
        assertEquals(3, dao.hent(id, "innsender")!!.antallSaker)
    }

    @Test
    fun `konkurrerende opprettelser returnerer samme batch`() {
        val noekkel = UUID.randomUUID()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(4)
        try {
            val futures =
                (1..4).map {
                    pool.submit(
                        Callable {
                            start.await()
                            opprett(noekkel)
                        },
                    )
                }
            start.countDown()
            val ids = futures.map { it.get() }
            assertEquals(1, ids.distinct().size)
            assertEquals(1, dao.hent(ids.first(), "innsender")!!.antallSaker)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `utloept lease gir ny eier og gammel eier kan aldri lagre`() {
        opprett()
        val foerste = dao.reserver()!!
        assertNull(dao.reserver())
        ds.connection.use {
            it
                .prepareStatement(
                    "UPDATE brev_batch_sak SET laas_til = clock_timestamp() - INTERVAL '1 second' WHERE utsendings_id = ?",
                ).use { stmt ->
                    stmt.setObject(1, foerste.resultat.utsendingsId)
                    stmt.executeUpdate()
                }
        }
        assertFalse(dao.oppdater(foerste, BrevBatchSakStatus.FERDIG, BrevBatchSteg.FERDIG))
        val andre = dao.reserver()!!
        assertEquals(foerste.resultat.utsendingsId, andre.resultat.utsendingsId)
        assertNotEquals(foerste.eier, andre.eier)
        assertEquals(2, andre.resultat.antallForsoek)
        assertFalse(dao.oppdater(foerste, BrevBatchSakStatus.FEILA, BrevBatchSteg.GRUNNLAG))
        assertTrue(dao.oppdater(andre, BrevBatchSakStatus.FERDIG, BrevBatchSteg.FERDIG, 42))
    }

    @Test
    fun `flere claimere reserverer hver sin sak og status summeres`() {
        val id = opprett(request = batchRequest(listOf(1, 2)))
        val foerste = dao.reserver()!!
        val andre = dao.reserver()!!
        assertNotEquals(foerste.resultat.utsendingsId, andre.resultat.utsendingsId)
        assertNull(dao.reserver())
        assertTrue(dao.oppdater(foerste, BrevBatchSakStatus.FERDIG, BrevBatchSteg.FERDIG))
        assertTrue(dao.oppdater(andre, BrevBatchSakStatus.FEILA, BrevBatchSteg.GRUNNLAG, feilkode = "SPRAAK_AVVIK"))
        val oversikt = dao.hent(id, "innsender")!!
        assertEquals(BrevBatchStatus.FERDIG_MED_FEIL, oversikt.status)
        assertEquals(1, oversikt.antallFerdige)
        assertEquals(1, oversikt.antallFeila)
    }

    @Test
    fun `snapshot er uforanderlig og beholdes etter utloept lease`() {
        opprett()
        val foerste = dao.reserver()!!
        val snapshot = snapshotRequest()
        assertTrue(dao.oppdater(foerste, BrevBatchSakStatus.UNDER_ARBEID, BrevBatchSteg.OPPRETT_BREV, snapshot = snapshot))
        assertTrue(
            dao.oppdater(
                foerste,
                BrevBatchSakStatus.UNDER_ARBEID,
                BrevBatchSteg.OPPRETT_BREV,
                snapshot = snapshot.copy(tittel = "Kan ikke overskrives"),
            ),
        )
        ds.connection.use {
            it
                .prepareStatement(
                    "UPDATE brev_batch_sak SET laas_til = clock_timestamp() - INTERVAL '1 second' WHERE utsendings_id = ?",
                ).use { stmt ->
                    stmt.setObject(1, foerste.resultat.utsendingsId)
                    stmt.executeUpdate()
                }
        }
        val andre = dao.reserver()!!
        assertEquals(snapshot, andre.snapshot)
        assertEquals(BrevBatchSteg.OPPRETT_BREV, andre.resultat.steg)
    }
}
