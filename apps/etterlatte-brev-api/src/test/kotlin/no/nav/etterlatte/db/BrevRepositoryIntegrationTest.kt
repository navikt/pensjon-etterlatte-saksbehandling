package no.nav.etterlatte.db

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRepositoryIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var db: BrevRepository
    private lateinit var dataSource: DataSource

    private val connection get() = dataSource.connection

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )
        dataSource.migrate()

        db = BrevRepository.using(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun resetTablesAfterEachTest() {
        connection.use {
            it.prepareStatement("TRUNCATE brev RESTART IDENTITY CASCADE;").execute()
        }
    }

    @Test
    fun `Enkel test med lagring og henting av brev`() {
        val behandlingId = UUID.randomUUID().toString()

        assertTrue(db.hentBrevForBehandling(behandlingId).isEmpty())

        val nyttBrev = opprettBrev(behandlingId)

        val brevTilBehandling = db.hentBrevForBehandling(behandlingId).single()
        assertEquals(nyttBrev.status, brevTilBehandling.status)

        val hentetBrev = db.hentBrev(nyttBrev.id)
        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(behandlingId, hentetBrev.behandlingId)
        assertEquals(hentetBrev.mottaker, opprettMottaker())
    }

    @Test
    fun `Lagring av flere brev paa flere behandlinger`() {
        val behandlingId = "1"

        opprettBrev(behandlingId)
        opprettBrev(behandlingId)
        opprettBrev(behandlingId)

        LongRange(2, 10).forEach {
            opprettBrev(it.toString())
        }

        assertEquals(3, db.hentBrevForBehandling(behandlingId).size)
    }

    @Test
    fun `Hent pdf for brev`() {
        val nyttBrev = opprettBrev("1")

        val innhold = db.hentBrevInnhold(nyttBrev.id)

        assertEquals(String(PDF_BYTES), String(innhold.data))
    }

    @Test
    fun `Slett brev`() {
        val behandlingId = "1"

        opprettBrev(behandlingId)
        opprettBrev(behandlingId)
        opprettBrev(behandlingId)

        val brev = db.hentBrevForBehandling(behandlingId)

        assertTrue(db.slett(brev.random().id))

        val resterendeBrev = db.hentBrevForBehandling(behandlingId)

        assertEquals(2, resterendeBrev.size)
    }

    @Test
    fun `Oppdater journalpost ID`() {
        val journalpostId = UUID.randomUUID().toString()

        val brev = opprettBrev("1")

        assertTrue(db.setJournalpostId(brev.id, journalpostId))
    }

    @Test
    fun `Oppdater bestilling ID`() {
        val journalpostId = UUID.randomUUID().toString()

        val brev = opprettBrev("1")

        assertTrue(db.setBestillingsId(brev.id, journalpostId))
    }

    @Test
    fun `Oppdater status`() {
        val opprettetBrev = opprettBrev("1")

        db.oppdaterStatus(opprettetBrev.id, Status.OPPDATERT)
        db.oppdaterStatus(opprettetBrev.id, Status.FERDIGSTILT)
        db.oppdaterStatus(opprettetBrev.id, Status.JOURNALFOERT)
        db.oppdaterStatus(opprettetBrev.id, Status.DISTRIBUERT)

        val count = connection.use {
            it.prepareStatement("SELECT COUNT(*) FROM hendelse WHERE brev_id = ${opprettetBrev.id}")
                .executeQuery()
                .let { rs ->
                    if (rs.next()) {
                        rs.getInt("count")
                    } else {
                        fail()
                    }
                }
        }

        // Skal være 5 hendelser. 1 for opprettet, og 4 for resten som ble kjørt manuelt
        assertEquals(5, count)
    }

    private fun opprettBrev(behandlingId: String): Brev {
        return db.opprettBrev(
            UlagretBrev(
                behandlingId = behandlingId,
                tittel = UUID.randomUUID().toString(),
                spraak = Spraak.NB,
                mottaker = opprettMottaker(),
                pdf = PDF_BYTES,
                erVedtaksbrev = false
            )
        )
    }

    private fun opprettMottaker() = Mottaker(
        adresse = Adresse(
            fornavn = "Test",
            etternavn = "Testesen",
            adresse = "Fyrstikkaleen 1",
            postnummer = "1234",
            poststed = "Oslo"
        )
    )

    companion object {
        private val PDF_BYTES = "Hello world!".toByteArray()
    }
}