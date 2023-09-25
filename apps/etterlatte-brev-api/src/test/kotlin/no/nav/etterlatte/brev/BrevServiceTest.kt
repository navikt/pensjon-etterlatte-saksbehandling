package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevService.BrevPayload
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random

internal class BrevServiceTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val brevbaker = mockk<BrevbakerKlient>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()
    private val distribusjonService = mockk<DistribusjonServiceImpl>()
    private val brevDataMapper = mockk<BrevDataMapper>()

    private val brevService =
        BrevService(
            db,
            sakOgBehandlingService,
            adresseService,
            dokarkivService,
            distribusjonService,
            BrevbakerService(brevbaker, adresseService, brevDataMapper),
        )

    private val bruker = BrukerTokenInfo.of(UUID.randomUUID().toString(), "Z123456", null, null, null)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, sakOgBehandlingService, adresseService, dokarkivService, distribusjonService, brevbaker)
    }

    @Nested
    inner class HentingAvBrev {
        @Test
        fun `Hent brev med ID`() {
            every { db.hentBrev(any()) } returns mockk()

            val id = Random.nextLong()
            val brev = brevService.hentBrev(id)
            brev shouldNotBe null

            verify {
                db.hentBrev(id)
            }
        }

        @Test
        fun `Hent brev tilhoerende sak`() {
            every { db.hentBrevForSak(any()) } returns
                listOf(
                    opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL),
                    opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL),
                    opprettBrev(Status.FERDIGSTILT, BrevProsessType.AUTOMATISK),
                )

            val sakId = Random.nextLong()
            val brevListe = brevService.hentBrevForSak(sakId)
            brevListe.size shouldBe 3

            verify {
                db.hentBrevForSak(sakId)
            }
        }
    }

    @Nested
    inner class PayloadManuelleBrev {
        @Test
        fun `Hent payload`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())
            every { db.hentBrev(any()) } returns brev

            val payload = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH)))
            every { db.hentBrevPayload(any()) } returns payload
            every { db.hentBrevPayloadVedlegg(any()) } returns null

            val faktiskPayload =
                runBlocking {
                    brevService.hentBrevPayload(brev.id)
                }

            faktiskPayload shouldBe BrevPayload(payload, null)

            verify {
                db.hentBrevPayload(brev.id)
                db.hentBrevPayloadVedlegg(brev.id)
            }
        }

        @Test
        fun `Hent payload - finnes ikke`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())
            every { db.hentBrevPayload(any()) } returns null
            every { db.hentBrevPayloadVedlegg(any()) } returns null

            val payload =
                runBlocking {
                    brevService.hentBrevPayload(brev.id)
                }

            payload shouldBe BrevPayload(null, null)

            verify {
                db.hentBrevPayload(brev.id)
                db.hentBrevPayloadVedlegg(brev.id)
            }
        }
    }

    @Nested
    inner class JournalfoeringAvBrev {
        @Test
        fun `Journalfoering fungerer som forventet`() {
            val brev = opprettBrev(Status.FERDIGSTILT, BrevProsessType.MANUELL)
            val sak = Sak("ident", SakType.BARNEPENSJON, brev.sakId, "1234")
            val journalpostResponse = JournalpostResponse("444", journalpostferdigstilt = true)

            coEvery { sakOgBehandlingService.hentSak(any(), any()) } returns sak
            coEvery { dokarkivService.journalfoer(any<Brev>(), any()) } returns journalpostResponse
            every { db.hentBrev(any()) } returns brev
            every { db.settBrevJournalfoert(any(), any()) } returns true

            val faktiskJournalpostId =
                runBlocking {
                    brevService.journalfoer(brev.id, bruker)
                }

            faktiskJournalpostId shouldBe journalpostResponse.journalpostId

            verify {
                db.hentBrev(brev.id)
                db.settBrevJournalfoert(brev.id, journalpostResponse)
            }
            coVerify {
                sakOgBehandlingService.hentSak(sak.id, bruker)
                dokarkivService.journalfoer(brev, sak)
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["FERDIGSTILT"],
        )
        fun `Journalfoering feiler hvis status er feil`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)
            every { db.hentBrev(any()) } returns brev

            runBlocking {
                assertThrows<IllegalStateException> {
                    brevService.journalfoer(brev.id, bruker)
                }
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    @Nested
    inner class DistribusjonAvBrev {
        @Test
        fun `Distribusjon fungerer som forventet`() {
            val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.MANUELL)
            val journalpostId = "1"

            every { db.hentBrev(any()) } returns brev
            every { db.hentJournalpostId(any()) } returns journalpostId
            every { distribusjonService.distribuerJournalpost(any(), any(), any(), any(), any()) } returns "123"

            val bestillingsID = brevService.distribuer(brev.id)
            bestillingsID shouldBe "123"

            verify {
                db.hentBrev(brev.id)
                db.hentJournalpostId(brev.id)
                distribusjonService.distribuerJournalpost(
                    brev.id,
                    journalpostId,
                    DistribusjonsType.ANNET,
                    DistribusjonsTidspunktType.KJERNETID,
                    brev.mottaker!!.adresse,
                )
            }
        }

        @Test
        fun `Distribusjon avbrytes hvis journalpostId mangler`() {
            val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev
            every { db.hentJournalpostId(any()) } returns null

            assertThrows<IllegalArgumentException> {
                brevService.distribuer(brev.id)
            }

            verify {
                db.hentBrev(brev.id)
                db.hentJournalpostId(brev.id)
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["JOURNALFOERT"],
        )
        fun `Distribusjon avbrytes ved feil status`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            assertThrows<IllegalStateException> {
                brevService.distribuer(brev.id)
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    private fun opprettBrev(
        status: Status,
        prosessType: BrevProsessType,
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = Random.nextLong(10000),
        behandlingId = null,
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        mottaker = opprettMottaker(),
    )

    private fun opprettMottaker() =
        Mottaker(
            "Stor Snerk",
            foedselsnummer = Foedselsnummer("1234567890"),
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
        )
}
