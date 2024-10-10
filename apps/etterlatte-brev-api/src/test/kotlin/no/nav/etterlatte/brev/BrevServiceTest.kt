package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.BrevService.BrevPayload
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
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
    val db = mockk<BrevRepository>(relaxed = true)
    val brevbaker = mockk<BrevbakerKlient>()
    val sakOgBehandlingService = mockk<BrevdataFacade>()
    val adresseService = mockk<AdresseService>()
    val journalfoerBrevService = mockk<JournalfoerBrevService>()
    val distribusjonService = mockk<DistribusjonServiceImpl>()
    val brevDataFacade = mockk<BrevdataFacade>()
    val pdfGenerator = mockk<PDFGenerator>()
    val brevbakerService = mockk<BrevbakerService>()
    val behandlingService = mockk<BehandlingService>()
    val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService, adresseService, behandlingService)
    val innholdTilRedigerbartBrevHenter =
        InnholdTilRedigerbartBrevHenter(brevDataFacade, brevbakerService, adresseService, redigerbartVedleggHenter)
    val brevoppretter =
        Brevoppretter(adresseService, db, innholdTilRedigerbartBrevHenter)

    val brevService =
        BrevService(
            db,
            brevoppretter,
            journalfoerBrevService,
            pdfGenerator,
            mockk(),
            mockk(),
        )

    val bruker = simpleSaksbehandler("Z123456")

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, sakOgBehandlingService, adresseService, journalfoerBrevService, distribusjonService, brevbaker)
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
                    opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR),
                    opprettBrev(Status.FERDIGSTILT, BrevProsessType.AUTOMATISK),
                )

            val sakId = randomSakId()
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
    inner class OppdateringAvBrev {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Oppdater tittel - status kan endres`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            val tittel = "En oppdatert tittel"
            brevService.oppdaterTittel(brev.id, tittel)

            verify {
                db.hentBrev(brev.id)
                db.oppdaterTittel(brev.id, tittel)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Oppdater tittel - kan IKKE endres`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                brevService.oppdaterTittel(brev.id, "Ny tittel skal feile")
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    @Nested
    inner class SlettingAvBrev {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Sletting av brev som er under arbeid skal virke`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            brevService.slett(brev.id, bruker)

            verify {
                db.hentBrev(brev.id)
                db.settBrevSlettet(brev.id, bruker)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Brev som ikke lenger er under arbeid skal IKKE kunne slettes`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                brevService.slett(brev.id, bruker)
            }

            verify {
                db.hentBrev(brev.id)
            }
            verify(exactly = 0) {
                db.settBrevSlettet(any(), any())
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class)
        fun `Skal ikke kunne slette vedtaksbrev, uavhengig av status`(status: Status) {
            val behandlingId = UUID.randomUUID()
            val brev = opprettBrev(status, BrevProsessType.MANUELL, behandlingId)

            every { db.hentBrev(any()) } returns brev

            assertThrows<Exception> {
                brevService.slett(brev.id, bruker)
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    private fun opprettBrev(
        status: Status,
        prosessType: BrevProsessType,
        behandlingId: UUID? = null,
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = randomSakId(),
        behandlingId = behandlingId,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        mottaker = opprettMottaker(),
        brevtype = Brevtype.INFORMASJON,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker() =
        Mottaker(
            "Stor Snerk",
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
        )
}
