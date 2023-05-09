package no.nav.etterlatte.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.postgresql.util.PSQLException
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
    fun `Henting av brev med ID fungerer`() {
        val antall = 10

        val brevListe = (1..antall).map {
            db.opprettBrev(ulagretBrev(UUID.randomUUID()))
        }

        brevListe.size shouldBeExactly 10

        brevListe.forEach {
            val brev = db.hentBrev(it.id)

            brev shouldBe it
        }
    }

    @Test
    fun `Hent brev med behandling ID`() {
        val behandlingId = UUID.randomUUID()

        assertNull(db.hentBrevForBehandling(behandlingId))

        val nyttBrev = db.opprettBrev(ulagretBrev(behandlingId))

        val brevTilBehandling = db.hentBrevForBehandling(behandlingId)!!
        assertEquals(nyttBrev.status, brevTilBehandling.status)

        val hentetBrev = db.hentBrev(nyttBrev.id)
        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(behandlingId, hentetBrev.behandlingId)
        assertEquals(hentetBrev.mottaker, opprettMottaker())
    }

    @Test
    fun `Opprett innhold ferdigstiller brev`() {
        val brev = db.opprettBrev(ulagretBrev(UUID.randomUUID()))

        brev.status shouldBe Status.OPPRETTET

        db.hentBrevInnhold(brev.id) shouldBe null

        db.opprettInnholdOgFerdigstill(brev.id, BrevInnhold(Spraak.NB, PDF_BYTES))

        val innhold = db.hentBrevInnhold(brev.id)!!
        innhold.spraak shouldBe Spraak.NB
        String(innhold.data) shouldBe String(PDF_BYTES)

        val hentetBrev = db.hentBrev(brev.id)

        hentetBrev.status shouldBe Status.FERDIGSTILT

        shouldThrow<PSQLException> {
            // Skal kun være mulig å lagre ETT innhold pr brev
            db.opprettInnholdOgFerdigstill(brev.id, BrevInnhold(Spraak.NB, PDF_BYTES))
        }
    }

    @Test
    fun `Slett brev`() {
        val behandlingId = UUID.randomUUID()

        db.opprettBrev(ulagretBrev(behandlingId))
        db.opprettBrev(ulagretBrev(behandlingId))
        db.opprettBrev(ulagretBrev(behandlingId))

        val brev = db.hentBrevForBehandling(behandlingId)!!

        assertTrue(db.slett(brev.id))
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

        db.opprettInnholdOgFerdigstill(opprettetBrev.id, BrevInnhold(Spraak.NB, "".toByteArray()))
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

    private fun ulagretBrev(behandlingId: UUID) = OpprettNyttBrev(
        behandlingId = behandlingId,
        soekerFnr = "00000012345",
        tittel = UUID.randomUUID().toString(),
        mottaker = opprettMottaker(),
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