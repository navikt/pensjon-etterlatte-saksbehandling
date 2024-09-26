package no.nav.etterlatte.brev.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.DatabaseExtension
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.postgresql.util.PSQLException
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRepositoryIntegrationTest(
    private val dataSource: DataSource,
) {
    private val db = BrevRepository(dataSource)

    @AfterEach
    fun resetTablesAfterEachTest() {
        dbExtension.resetDb()
    }

    @Test
    fun `Henting av brev med ID fungerer`() {
        val antall = 10

        val brevListe =
            (1..antall).map {
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

        assertEquals(emptyList<Brev>(), db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK))

        val nyttBrev = db.opprettBrev(ulagretBrev(behandlingId = behandlingId))

        val brevTilBehandling = db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).first()
        assertEquals(nyttBrev.status, brevTilBehandling.status)

        val hentetBrev = db.hentBrev(nyttBrev.id)
        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(behandlingId, hentetBrev.behandlingId)
        assertEquals(hentetBrev.mottaker, opprettMottaker())
    }

    @Test
    fun `Hent brev med sak_id`() {
        val sakId = randomSakId()

        repeat(10) {
            // Opprette brev for andre saker
            db.opprettBrev(ulagretBrev(randomSakId()))
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
    fun `Lagring av pdf separat`() {
        val ulagretBrev = ulagretBrev(behandlingId = UUID.randomUUID())
        val brev = db.opprettBrev(ulagretBrev)

        brev.status shouldBe Status.OPPRETTET

        db.hentBrevInnhold(brev.id) shouldBe ulagretBrev.innhold
        db.lagrePdf(brev.id, Pdf(PDF_BYTES))

        val pdf = db.hentPdf(brev.id)!!
        pdf.bytes.contentEquals(PDF_BYTES) shouldBe true

        val hentetBrev = db.hentBrev(brev.id)

        hentetBrev.status shouldBe Status.OPPRETTET
    }

    @Test
    fun `Sette status ferdigstilt`() {
        val ulagretBrev = ulagretBrev(behandlingId = UUID.randomUUID())
        val brev = db.opprettBrev(ulagretBrev)

        brev.status shouldBe Status.OPPRETTET

        db.oppdaterPayload(brev.id, Slate())
        db.settBrevFerdigstilt(brev.id)

        val hentetBrev = db.hentBrev(brev.id)
        hentetBrev.status shouldBe Status.FERDIGSTILT
    }

    @Test
    fun `Endre status ved underkjent vedtak`() {
        val ulagretBrev = ulagretBrev(behandlingId = UUID.randomUUID())
        val brev = db.opprettBrev(ulagretBrev)

        brev.status shouldBe Status.OPPRETTET

        db.oppdaterPayload(brev.id, Slate())
        db.settBrevFerdigstilt(brev.id)

        val ferdigstiltBrev = db.hentBrev(brev.id)
        ferdigstiltBrev.status shouldBe Status.FERDIGSTILT

        db.fjernFerdigstiltStatusUnderkjentVedtak(brev.id, """{"key":"value"}""".toJsonNode())
        val underkjentBrev = db.hentBrev(brev.id)
        underkjentBrev.status shouldBe Status.OPPDATERT
    }

    @Test
    fun `Oppdater journalpost ID`() {
        val journalpostId = UUID.randomUUID().toString()

        val brev = db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))

        assertTrue(db.settBrevJournalfoert(brev.id, OpprettJournalpostResponse(journalpostId, journalpostferdigstilt = true)))
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

        db.settBrevJournalfoert(opprettetBrev.id, OpprettJournalpostResponse("id", journalpostferdigstilt = true))
        db.hentBrev(opprettetBrev.id).status shouldBe Status.JOURNALFOERT

        db.settBrevDistribuert(opprettetBrev.id, DistribuerJournalpostResponse("id"))
        db.hentBrev(opprettetBrev.id).status shouldBe Status.DISTRIBUERT

        val antallHendelser =
            using(sessionOf(dataSource)) {
                it.run(
                    queryOf(
                        "SELECT COUNT(*) FROM hendelse WHERE brev_id = ?",
                        opprettetBrev.id,
                    ).map { row -> row.int("count") }.asSingle,
                )
            }

        antallHendelser shouldBe 5
    }

    @Test
    fun `Hent journalpost_id`() {
        val opprettetBrev = db.opprettBrev(ulagretBrev(behandlingId = UUID.randomUUID()))
        db.lagrePdfOgFerdigstillBrev(opprettetBrev.id, Pdf(PDF_BYTES))

        val journalpostResponse = OpprettJournalpostResponse("id", journalpostferdigstilt = true)
        db.settBrevJournalfoert(opprettetBrev.id, journalpostResponse)

        db.hentBrev(opprettetBrev.id).status shouldBe Status.JOURNALFOERT
        db.hentJournalpostId(opprettetBrev.id) shouldBe journalpostResponse.journalpostId
    }

    @Test
    fun `Oppdater tittel`() {
        val nyttBrev = db.opprettBrev(ulagretBrev())

        val nyTittel = "En helt ny tittel"
        assertNotEquals(nyTittel, nyttBrev.tittel)

        db.oppdaterTittel(nyttBrev.id, tittel = nyTittel)

        val brevMedOppdatertTittel = db.hentBrev(nyttBrev.id)

        assertEquals(nyTittel, brevMedOppdatertTittel.tittel)
    }

    @Test
    fun `Sletting av brev fungerer som forventet`() {
        val sakId = randomSakId()

        repeat(9) {
            db.opprettBrev(ulagretBrev(sakId))
        }

        val brevSkalSlettes = db.opprettBrev(ulagretBrev(sakId))

        val brevForSak = db.hentBrevForSak(sakId)
        assertEquals(10, brevForSak.size)

        db.settBrevSlettet(brevSkalSlettes.id, mockk(relaxed = true))

        val brevForSakEtterSletting = db.hentBrevForSak(sakId)
        assertEquals(9, brevForSakEtterSletting.size)

        val slettetBrev = db.hentBrev(brevSkalSlettes.id)
        assertEquals(Status.SLETTET, slettetBrev.status)
    }

    @Nested
    inner class TestInnholdPayload {
        @Test
        fun `Opprett og hent brev payload`() {
            val ulagretBrevUtenPayload =
                ulagretBrev(
                    behandlingId = UUID.randomUUID(),
                    innhold = BrevInnhold("tittel", Spraak.NB, payload = null),
                )
            val brevUtenPayload = db.opprettBrev(ulagretBrevUtenPayload)

            brevUtenPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevUtenPayload.id) shouldBe ulagretBrevUtenPayload.innhold
            db.hentBrevPayload(brevUtenPayload.id) shouldBe null // Automatisk brev skal IKKE ha payload

            val ulagretBrevMedPayload =
                ulagretBrev(
                    behandlingId = UUID.randomUUID(),
                    innhold =
                        BrevInnhold(
                            "tittel",
                            Spraak.NB,
                            payload = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH))),
                        ),
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
                            listOf(Slate.InnerElement(text = "Hello world!")),
                        ),
                    ),
                ),
            )

            val payload = db.hentBrevPayload(opprettetBrev.id)!!
            payload.elements.size shouldBeExactly 1
            payload.elements[0].type shouldBe Slate.ElementType.HEADING_TWO
        }

        @Test
        fun `Opprett og hent vedlegg payload`() {
            val ulagretBrevUtenPayload =
                ulagretBrev(
                    behandlingId = UUID.randomUUID(),
                    innhold = BrevInnhold("tittel", Spraak.NB, payload = null),
                    innhold_vedlegg = null,
                )
            val brevUtenPayload = db.opprettBrev(ulagretBrevUtenPayload)

            brevUtenPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevUtenPayload.id) shouldBe ulagretBrevUtenPayload.innhold
            db.hentBrevPayloadVedlegg(brevUtenPayload.id) shouldBe null // Automatisk brev skal IKKE ha payload

            val ulagretBrevMedPayload =
                ulagretBrev(
                    behandlingId = UUID.randomUUID(),
                    innhold_vedlegg =
                        listOf(
                            BrevInnholdVedlegg(
                                tittel = "Tittel",
                                key = BrevVedleggKey.OMS_BEREGNING,
                                payload = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH))),
                            ),
                        ),
                )
            val brevMedPayload = db.opprettBrev(ulagretBrevMedPayload)

            brevMedPayload.status shouldBe Status.OPPRETTET

            db.hentBrevInnhold(brevMedPayload.id) shouldBe ulagretBrevMedPayload.innhold
            db.hentBrevPayloadVedlegg(brevMedPayload.id) shouldBe ulagretBrevMedPayload.innholdVedlegg
        }

        @Test
        fun `Oppdatering av vedlegg payload`() {
            val ulagretBrev =
                ulagretBrev(
                    behandlingId = UUID.randomUUID(),
                    innhold_vedlegg =
                        listOf(
                            BrevInnholdVedlegg(
                                tittel = "tittel",
                                key = BrevVedleggKey.OMS_BEREGNING,
                                payload = null,
                            ),
                        ),
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
                        key = BrevVedleggKey.OMS_BEREGNING,
                        payload =
                            Slate(
                                listOf(
                                    Slate.Element(
                                        Slate.ElementType.HEADING_TWO,
                                        listOf(Slate.InnerElement(text = "Hello world!")),
                                    ),
                                ),
                            ),
                    ),
                ),
            )

            val vedleggPayload = db.hentBrevPayloadVedlegg(opprettetBrev.id)!!
            vedleggPayload
                .first()
                .payload!!
                .elements.size shouldBeExactly 1
            vedleggPayload
                .first()
                .payload!!
                .elements[0]
                .type shouldBe Slate.ElementType.HEADING_TWO
        }
    }

    private fun ulagretBrev(
        sakId: SakId = randomSakId(),
        behandlingId: UUID? = null,
        innhold: BrevInnhold? = null,
        innhold_vedlegg: List<BrevInnholdVedlegg>? = null,
    ) = OpprettNyttBrev(
        sakId = sakId,
        behandlingId = behandlingId,
        prosessType = BrevProsessType.AUTOMATISK,
        soekerFnr = "00000012345",
        mottaker = opprettMottaker(),
        opprettet = Tidspunkt.now(),
        innhold = innhold ?: BrevInnhold("tittel", Spraak.NB),
        innholdVedlegg = innhold_vedlegg,
        brevtype = Brevtype.VEDTAK,
        brevkoder = Brevkoder.BP_INNVILGELSE,
    )

    private fun opprettMottaker() =
        Mottaker(
            navn = "Test Testesen",
            foedselsnummer = STOR_SNERK,
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Fyrstikkaleen 1",
                    postnummer = "1234",
                    poststed = "Oslo",
                    land = "Norge",
                    landkode = "NOR",
                ),
        )

    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()

        private val PDF_BYTES = "Hello world!".toByteArray()
        private val STOR_SNERK = MottakerFoedselsnummer("11057523044")
    }
}
