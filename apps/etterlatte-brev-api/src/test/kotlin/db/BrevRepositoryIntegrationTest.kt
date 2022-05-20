package db

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.db.Adresse
import no.nav.etterlatte.db.Brev
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.db.Mottaker
import no.nav.etterlatte.db.NyttBrev
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource
import kotlin.random.Random

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

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
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
        val behandlingId = Random.nextLong()

        assertTrue(db.hentBrevForBehandling(behandlingId).isEmpty())

        val nyttBrev = opprettBrev(behandlingId)

        val brevTilBehandling = db.hentBrevForBehandling(behandlingId).single()
        assertNull(brevTilBehandling.data)
        assertEquals(nyttBrev.status, brevTilBehandling.status)

        val hentetBrev = db.hentBrev(nyttBrev.id)
        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(behandlingId, hentetBrev.behandlingId)
        assertNull(hentetBrev.data)
    }

    @Test
    fun `Lagring av flere brev paa flere behandlinger`() {
        val behandlingId = 1L

        opprettBrev(behandlingId)
        opprettBrev(behandlingId)
        opprettBrev(behandlingId)

        LongRange(2, 10).forEach {
            opprettBrev(it)
        }

        assertEquals(3, db.hentBrevForBehandling(behandlingId).size)
    }

    @Test
    fun `Hent pdf for brev`() {
        val nyttBrev = opprettBrev(1)

        assertNull(nyttBrev.data)

        val innhold = db.hentBrevInnhold(nyttBrev.id)

        assertEquals(String(PDF_BYTES), String(innhold.data))
    }

    @Test
    fun `Slett brev`() {
        val behandlingId = 1L

        opprettBrev(behandlingId)
        opprettBrev(behandlingId)
        opprettBrev(behandlingId)

        val brev = db.hentBrevForBehandling(behandlingId)

        assertTrue(db.slett(brev.random().id))

        val resterendeBrev = db.hentBrevForBehandling(behandlingId)

        assertEquals(2, resterendeBrev.size)
    }

    private fun opprettBrev(behandlingId: Long): Brev {
        return db.opprettBrev(
            NyttBrev(
                behandlingId = behandlingId,
                tittel = UUID.randomUUID().toString(),
                mottaker = opprettMottaker(),
                pdf = PDF_BYTES
            )
        )
    }

    private fun opprettMottaker() = Mottaker(
        fornavn = "Test",
        etternavn = "Testesen",
        foedselsnummer = null,
        adresse = Adresse(
            adresse = "Fyrstikkaleen 1",
            postnummer = "1234",
            poststed = "Oslo"
        )
    )

    companion object {
        private val PDF_BYTES = "Hello world!".toByteArray()
    }
}
