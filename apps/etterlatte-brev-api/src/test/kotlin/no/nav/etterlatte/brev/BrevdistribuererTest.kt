package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.distribusjon.FeilStatusForDistribusjon
import no.nav.etterlatte.brev.distribusjon.JournalpostIdMangler
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random

internal class BrevdistribuererTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val distribusjonService = mockk<DistribusjonServiceImpl>()

    private val brevdistribuerer = Brevdistribuerer(db, distribusjonService)

    private val bruker = simpleSaksbehandler("Z123456")

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(db, distribusjonService)
    }

    @Test
    fun `Distribusjon fungerer som forventet (kun 1 mottaker)`() {
        val journalpostId = Random.nextLong().toString()
        val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.REDIGERBAR, listOf(opprettMottaker(journalpostId)))

        val response = DistribuerJournalpostResponse(Random.nextLong().toString())

        every { db.hentBrev(any()) } returns brev
        every {
            distribusjonService.distribuerJournalpost(
                any(),
                any(),
                any(),
                any(),
            )
        } returns response

        val bestillingsID = brevdistribuerer.distribuer(brev.id, bruker = bruker)
        bestillingsID shouldBe listOf(response.bestillingsId)

        verify {
            db.hentBrev(brev.id)
            db.lagreBestillingId(brev.mottakere.single().id, response)

            db.settBrevDistribuert(brev.id, listOf(response), bruker)

            distribusjonService.distribuerJournalpost(
                brev.id,
                DistribusjonsType.ANNET,
                brev.mottakere.single(),
                any(),
            )
        }
    }

    @Test
    fun `Distribusjon fungerer som forventet (flere mottakere)`() {
        val mottaker1 = opprettMottaker(journalpostId = Random.nextLong().toString())
        val mottaker2 = opprettMottaker(journalpostId = Random.nextLong().toString())

        val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.REDIGERBAR, listOf(mottaker1, mottaker2))

        val response1 = DistribuerJournalpostResponse("1")
        val response2 = DistribuerJournalpostResponse("2")

        every { db.hentBrev(any()) } returns brev
        every {
            distribusjonService.distribuerJournalpost(any(), any(), match { it.id == mottaker1.id }, any())
        } returns response1
        every {
            distribusjonService.distribuerJournalpost(any(), any(), match { it.id == mottaker2.id }, any())
        } returns response2

        brevdistribuerer.distribuer(brev.id, bruker = bruker) shouldBe listOf(response1.bestillingsId, response2.bestillingsId)

        verify(exactly = 1) {
            db.hentBrev(brev.id)

            db.lagreBestillingId(brev.mottakere[0].id, DistribuerJournalpostResponse("1"))
            db.lagreBestillingId(brev.mottakere[1].id, DistribuerJournalpostResponse("2"))

            db.settBrevDistribuert(brev.id, listOf(response1, response2), bruker)

            distribusjonService.distribuerJournalpost(brev.id, DistribusjonsType.ANNET, brev.mottakere[0], any())
            distribusjonService.distribuerJournalpost(brev.id, DistribusjonsType.ANNET, brev.mottakere[1], any())
        }
    }

    @Test
    fun `Distribusjon avbrytes hvis journalpostId mangler`() {
        val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.REDIGERBAR, listOf(opprettMottaker()))

        every { db.hentBrev(any()) } returns brev

        assertThrows<JournalpostIdMangler> {
            brevdistribuerer.distribuer(brev.id, bruker = bruker)
        }

        verify {
            db.hentBrev(brev.id)
        }
    }

    @ParameterizedTest
    @EnumSource(
        Status::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["JOURNALFOERT"],
    )
    fun `Distribusjon avbrytes ved feil status`(status: Status) {
        val brev = opprettBrev(status, BrevProsessType.REDIGERBAR, listOf(opprettMottaker()))

        every { db.hentBrev(any()) } returns brev

        if (status != Status.DISTRIBUERT) {
            assertThrows<FeilStatusForDistribusjon> {
                brevdistribuerer.distribuer(brev.id, bruker = bruker)
            }
        } else {
            assertDoesNotThrow {
                brevdistribuerer.distribuer(brev.id, bruker = bruker)
            }
        }

        verify {
            db.hentBrev(brev.id)
        }
    }

    private fun opprettBrev(
        status: Status,
        prosessType: BrevProsessType,
        mottakere: List<Mottaker>,
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = randomSakId(),
        behandlingId = null,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        mottakere = mottakere,
        brevtype = Brevtype.MANUELT,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker(journalpostId: String? = null) =
        Mottaker(
            id = UUID.randomUUID(),
            navn = "Stor Snerk",
            foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
            orgnummer = null,
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Testgaten 13",
                    postnummer = "1234",
                    poststed = "OSLO",
                    land = "Norge",
                    landkode = "NOR",
                ),
            journalpostId = journalpostId,
        )
}
