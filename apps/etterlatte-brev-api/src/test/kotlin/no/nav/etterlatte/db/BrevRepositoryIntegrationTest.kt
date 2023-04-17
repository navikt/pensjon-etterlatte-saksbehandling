package no.nav.etterlatte.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRepositoryIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var db: BrevRepository
    private lateinit var dataSource: DataSource

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

        db = BrevRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun resetTablesAfterEachTest() {
        using(sessionOf(dataSource)) {
            it.run(queryOf("TRUNCATE brev RESTART IDENTITY CASCADE;").asExecute)
        }
    }

    @Test
    fun `Enkel test med lagring og henting av brev`() {
        val behandlingId = UUID.randomUUID()

        assertTrue(db.hentBrevForBehandling(behandlingId).isEmpty())

        val nyttBrev = db.opprettBrev(ulagretBrev(behandlingId))

        val brevTilBehandling = db.hentBrevForBehandling(behandlingId).single()
        assertEquals(nyttBrev.status, brevTilBehandling.status)

        val hentetBrev = db.hentBrev(nyttBrev.id)
        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(behandlingId, hentetBrev.behandlingId)
        assertEquals(hentetBrev.mottaker, opprettMottaker())
    }

    @Test
    fun `Lagring av flere brev paa flere behandlinger`() {
        val behandlingId = UUID.randomUUID()

        val brev1 = db.opprettBrev(ulagretBrev(behandlingId))
        assertEquals(1, brev1.id)

        val brev2 = db.opprettBrev(ulagretBrev(behandlingId))
        assertEquals(2, brev2.id)

        val brev3 = db.opprettBrev(ulagretBrev(behandlingId))
        assertEquals(3, brev3.id)

        repeat(10) {
            val nyttBrev = ulagretBrev(UUID.randomUUID())
            val lagretBrev = db.opprettBrev(nyttBrev)
            val hentetBrev = db.hentBrev(lagretBrev.id)

            assertEquals(lagretBrev, hentetBrev)
        }

        assertEquals(3, db.hentBrevForBehandling(behandlingId).size)
    }

    @Test
    fun `Hent pdf for brev`() {
        val nyttBrev = db.opprettBrev(ulagretBrev(UUID.randomUUID()))

        val innhold = db.hentBrevInnhold(nyttBrev.id)

        assertEquals(String(PDF_BYTES), String(innhold.data))
    }

    @Test
    fun `Slett brev`() {
        val behandlingId = UUID.randomUUID()

        db.opprettBrev(ulagretBrev(behandlingId))
        db.opprettBrev(ulagretBrev(behandlingId))
        db.opprettBrev(ulagretBrev(behandlingId))

        val brev = db.hentBrevForBehandling(behandlingId)

        assertTrue(db.slett(brev.random().id))

        val resterendeBrev = db.hentBrevForBehandling(behandlingId)

        assertEquals(2, resterendeBrev.size)
    }

    @Test
    fun `Oppdater journalpost ID`() {
        val journalpostId = UUID.randomUUID().toString()

        val brev = db.opprettBrev(ulagretBrev(UUID.randomUUID()))

        assertTrue(db.settBrevJournalfoert(brev.id, JournalpostResponse(journalpostId, journalpostferdigstilt = true)))
    }

    @Test
    fun `Oppdater bestilling ID`() {
        val bestillingsId = UUID.randomUUID().toString()

        val brev = db.opprettBrev(ulagretBrev(UUID.randomUUID()))

        assertTrue(db.settBrevDistribuert(brev.id, DistribuerJournalpostResponse(bestillingsId)))
    }

    @Test
    fun `Oppdater status`() {
        val opprettetBrev = db.opprettBrev(ulagretBrev(UUID.randomUUID()))

        db.oppdaterBrev(opprettetBrev.id, ulagretBrev(opprettetBrev.behandlingId))
        db.settBrevFerdigstilt(opprettetBrev.id)
        db.settBrevJournalfoert(opprettetBrev.id, JournalpostResponse("id", journalpostferdigstilt = true))
        db.settBrevDistribuert(opprettetBrev.id, DistribuerJournalpostResponse("id"))

        val count =
            sessionOf(dataSource).use {
                it.run(
                    queryOf(
                        "SELECT COUNT(*) FROM hendelse WHERE brev_id = ?",
                        opprettetBrev.id
                    ).map { row -> row.int("count") }.asSingle
                )
            }

        // Skal være 5 hendelser. 1 for opprettet, og 4 for resten som ble kjørt manuelt
        assertEquals(5, count)
    }

    @Test
    fun `Oppdater brev`() {
        val behandlingId = UUID.randomUUID()

        val originaltBrev = ulagretBrev(behandlingId)
        val opprettetBrev = db.opprettBrev(originaltBrev)

        db.oppdaterBrev(opprettetBrev.id, originaltBrev)

        val faktiskBrev = db.hentBrev(opprettetBrev.id)

        assertEquals(Status.OPPDATERT, faktiskBrev.status)
    }

    private fun ulagretBrev(behandlingId: UUID) = UlagretBrev(
        behandlingId = behandlingId,
        soekerFnr = "00000012345",
        tittel = UUID.randomUUID().toString(),
        spraak = Spraak.NB,
        mottaker = opprettMottaker(),
        pdf = PDF_BYTES,
        erVedtaksbrev = false
    )

    private fun opprettMottaker() = Mottaker(
        navn = "Test Testesen",
        foedselsnummer = STOR_SNERK,
        adresse = Adresse(
            adresseType = "NORSKPOSTADRESSE",
            adresselinje1 = "Fyrstikkaleen 1",
            postnummer = "1234",
            poststed = "Oslo",
            land = "Norge",
            landkode = "NOR"
        )
    )

    companion object {
        private val PDF_BYTES = "Hello world!".toByteArray()
        private val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
    }
}