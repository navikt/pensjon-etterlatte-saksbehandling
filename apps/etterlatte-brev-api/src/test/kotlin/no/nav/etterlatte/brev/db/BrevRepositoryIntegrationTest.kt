package no.nav.etterlatte.brev.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.postgresql.util.PSQLException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRepositoryIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

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
            db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))
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

        val nyttBrev = db.opprettBrev(ulagretBrev(behandlingId = behandlingId))

        val brevTilBehandling = db.hentBrevForBehandling(behandlingId)!!
        assertEquals(nyttBrev.status, brevTilBehandling.status)

        val hentetBrev = db.hentBrev(nyttBrev.id)
        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(behandlingId, hentetBrev.behandlingId)
        assertEquals(hentetBrev.mottaker, opprettMottaker())
    }

    @Test
    fun `Hent brev med sak_id`() {
        val sakId = Random.nextLong()

        repeat(10) {
            // Opprette brev for andre saker
            db.opprettBrev(ulagretBrev(Random.nextLong()))
        }

        db.hentBrevForSak(sakId) shouldBe emptyList()

        val forventetAntall = 7
        repeat(forventetAntall) {
            db.opprettBrev(ulagretBrev(sakId = sakId))
        }

        val brevForSak = db.hentBrevForSak(sakId)
        brevForSak.size shouldBe forventetAntall
    }

    @Test
    fun `Lagring av pdf skal ferdigstille brev`() {
        val ulagretBrev = ulagretBrev(behandlingId = UUID.randomUUID())
        val brev = db.opprettBrev(ulagretBrev)

        brev.status shouldBe Status.OPPRETTET

        db.hentBrevInnhold(brev.id) shouldBe ulagretBrev.innhold
        db.lagrePdfOgFerdigstillBrev(brev.id, Pdf(PDF_BYTES))

        val pdf = db.hentPdf(brev.id)!!
        pdf.bytes.contentEquals(PDF_BYTES) shouldBe true

        val hentetBrev = db.hentBrev(brev.id)

        hentetBrev.status shouldBe Status.FERDIGSTILT

        shouldThrow<PSQLException> {
            // Skal kun være mulig å lagre ETT pdf-dokument pr brev
            db.lagrePdfOgFerdigstillBrev(brev.id, Pdf(PDF_BYTES))
        }
    }

    @Test
    fun `Slett brev`() {
        val behandlingId = UUID.randomUUID()

        db.opprettBrev(ulagretBrev(behandlingId = behandlingId))

        val brev = db.hentBrevForBehandling(behandlingId)!!

        assertTrue(db.slett(brev.id))
    }

    @Test
    fun `Oppdater journalpost ID`() {
        val journalpostId = UUID.randomUUID().toString()

        val brev = db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))

        assertTrue(db.settBrevJournalfoert(brev.id, JournalpostResponse(journalpostId, journalpostferdigstilt = true)))
    }

    @Test
    fun `Oppdater bestilling ID`() {
        val bestillingsId = UUID.randomUUID().toString()

        val brev = db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))

        assertTrue(db.settBrevDistribuert(brev.id, DistribuerJournalpostResponse(bestillingsId)))
    }

    @Test
    fun `Oppdater status`() {
        val opprettetBrev = db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))
        db.hentBrev(opprettetBrev.id).status shouldBe Status.OPPRETTET

        db.oppdaterPayload(opprettetBrev.id, Slate())
        db.hentBrev(opprettetBrev.id).status shouldBe Status.OPPDATERT

        db.lagrePdfOgFerdigstillBrev(opprettetBrev.id, Pdf(PDF_BYTES))
        db.hentBrev(opprettetBrev.id).status shouldBe Status.FERDIGSTILT

        db.settBrevJournalfoert(opprettetBrev.id, JournalpostResponse("id", journalpostferdigstilt = true))
        db.hentBrev(opprettetBrev.id).status shouldBe Status.JOURNALFOERT

        db.settBrevDistribuert(opprettetBrev.id, DistribuerJournalpostResponse("id"))
        db.hentBrev(opprettetBrev.id).status shouldBe Status.DISTRIBUERT

        val antallHendelser =
            using(sessionOf(dataSource)) {
                it.run(
                    queryOf(
                        "SELECT COUNT(*) FROM hendelse WHERE brev_id = ?",
                        opprettetBrev.id
                    ).map { row -> row.int("count") }.asSingle
                )
            }

        antallHendelser shouldBe 5
    }

    @Test
    fun `Hent journalpost_id`() {
        val opprettetBrev = db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))
        db.lagrePdfOgFerdigstillBrev(opprettetBrev.id, Pdf(PDF_BYTES))

        val journalpostResponse = JournalpostResponse("id", journalpostferdigstilt = true)
        db.settBrevJournalfoert(opprettetBrev.id, journalpostResponse)

        db.hentBrev(opprettetBrev.id).status shouldBe Status.JOURNALFOERT
        db.hentJournalpostId(opprettetBrev.id) shouldBe journalpostResponse.journalpostId
    }

    @Nested
    inner class TestInnholdPayload {
        @Test
        fun `Opprett og hent brev payload`() {
            val ulagretBrevUtenPayload = ulagretBrev(
                behandlingId = UUID.randomUUID(),
                innhold = BrevInnhold("tittel", Spraak.NB, payload = null)
            )
            val brevUtenPayload = db.opprettBrev(ulagretBrevUtenPayload)

            brevUtenPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevUtenPayload.id) shouldBe ulagretBrevUtenPayload.innhold
            db.hentBrevPayload(brevUtenPayload.id) shouldBe null // Automatisk brev skal IKKE ha payload

            val ulagretBrevMedPayload = ulagretBrev(
                behandlingId = UUID.randomUUID(),
                innhold = BrevInnhold(
                    "tittel",
                    Spraak.NB,
                    payload = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH)))
                )
            )
            val brevMedPayload = db.opprettBrev(ulagretBrevMedPayload)

            brevMedPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevMedPayload.id) shouldBe ulagretBrevMedPayload.innhold
            db.hentBrevPayload(brevMedPayload.id) shouldBe ulagretBrevMedPayload.innhold.payload
        }

        @Test
        fun `Oppdatering av payload`() {
            val ulagretBrev = ulagretBrev(behandlingId = UUID.randomUUID())
            val opprettetBrev = db.opprettBrev(ulagretBrev)

            db.oppdaterPayload(opprettetBrev.id, Slate(emptyList()))

            val initialPayload = db.hentBrevPayload(opprettetBrev.id)

            initialPayload shouldNotBe null

            db.hentBrev(opprettetBrev.id).status shouldBe Status.OPPDATERT

            db.oppdaterPayload(
                opprettetBrev.id,
                Slate(
                    listOf(
                        Slate.Element(
                            Slate.ElementType.HEADING_TWO,
                            listOf(Slate.InnerElement(text = "Hello world!"))
                        )
                    )
                )
            )

            val payload = db.hentBrevPayload(opprettetBrev.id)!!
            payload.elements.size shouldBeExactly 1
            payload.elements[0].type shouldBe Slate.ElementType.HEADING_TWO
        }

        @Test
        fun `Opprett og hent vedlegg payload`() {
            val ulagretBrevUtenPayload = ulagretBrev(
                behandlingId = UUID.randomUUID(),
                innhold = BrevInnhold("tittel", Spraak.NB, payload = null),
                innhold_vedlegg = null
            )
            val brevUtenPayload = db.opprettBrev(ulagretBrevUtenPayload)

            brevUtenPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevUtenPayload.id) shouldBe ulagretBrevUtenPayload.innhold
            db.hentBrevPayloadVedlegg(brevUtenPayload.id) shouldBe null // Automatisk brev skal IKKE ha payload

            val ulagretBrevMedPayload = ulagretBrev(
                behandlingId = UUID.randomUUID(),
                innhold_vedlegg = listOf(
                    BrevInnholdVedlegg(
                        tittel = "Tittel",
                        key = "brev_vedlegg",
                        payload = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH)))
                    )
                )
            )
            val brevMedPayload = db.opprettBrev(ulagretBrevMedPayload)

            brevMedPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevMedPayload.id) shouldBe ulagretBrevMedPayload.innhold
            db.hentBrevPayloadVedlegg(brevMedPayload.id) shouldBe ulagretBrevMedPayload.innholdVedlegg
        }

        @Test
        fun `Oppdatering av vedlegg payload`() {
            val ulagretBrev = ulagretBrev(
                behandlingId = UUID.randomUUID(),
                innhold_vedlegg = listOf(
                    BrevInnholdVedlegg(
                        tittel = "tittel",
                        key = "brev_vedlegg",
                        payload = null
                    )
                )
            )
            val opprettetBrev = db.opprettBrev(ulagretBrev)

            db.oppdaterPayload(opprettetBrev.id, Slate(emptyList()))

            val initialPayload = db.hentBrevPayloadVedlegg(opprettetBrev.id)

            initialPayload shouldNotBe null

            db.hentBrev(opprettetBrev.id).status shouldBe Status.OPPDATERT

            db.oppdaterPayloadVedlegg(
                opprettetBrev.id,
                listOf(
                    BrevInnholdVedlegg(
                        tittel = "tittel",
                        key = "brev_vedlegg",
                        payload = Slate(
                            listOf(
                                Slate.Element(
                                    Slate.ElementType.HEADING_TWO,
                                    listOf(Slate.InnerElement(text = "Hello world!"))
                                )
                            )
                        )
                    )
                )
            )

            val vedleggPayload = db.hentBrevPayloadVedlegg(opprettetBrev.id)!!
            vedleggPayload.first().payload!!.elements.size shouldBeExactly 1
            vedleggPayload.first().payload!!.elements[0].type shouldBe Slate.ElementType.HEADING_TWO
        }
    }

    private fun ulagretBrev(
        sakId: Long = Random.nextLong(),
        behandlingId: UUID? = null,
        innhold: BrevInnhold? = null,
        innhold_vedlegg: List<BrevInnholdVedlegg>? = null
    ) = OpprettNyttBrev(
        sakId = sakId,
        behandlingId = behandlingId,
        prosessType = BrevProsessType.AUTOMATISK,
        soekerFnr = "00000012345",
        mottaker = opprettMottaker(),
        innhold = innhold ?: BrevInnhold("tittel", Spraak.NB),
        innholdVedlegg = innhold_vedlegg ?: null
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
        private val STOR_SNERK = Foedselsnummer("11057523044")
    }
}