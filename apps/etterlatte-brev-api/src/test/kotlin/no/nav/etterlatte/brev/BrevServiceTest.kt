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
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

internal class BrevServiceTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val brevbaker = mockk<BrevbakerKlient>()
    private val sakOgBehandlingService = mockk<BrevdataFacade>()
    private val adresseService = mockk<AdresseService>()
    private val journalfoerBrevService = mockk<JournalfoerBrevService>()
    private val distribusjonService = mockk<DistribusjonServiceImpl>()
    private val brevDataFacade = mockk<BrevdataFacade>()
    private val pdfGenerator = mockk<PDFGenerator>()
    private val brevbakerService = mockk<BrevbakerService>()
    private val behandlingService = mockk<BehandlingService>()
    private val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService, adresseService, behandlingService)
    private val innholdTilRedigerbartBrevHenter =
        InnholdTilRedigerbartBrevHenter(brevDataFacade, brevbakerService, adresseService, redigerbartVedleggHenter)
    private val brevoppretter =
        Brevoppretter(adresseService, db, innholdTilRedigerbartBrevHenter)

    private val brevService =
        BrevService(
            db,
            brevoppretter,
            journalfoerBrevService,
            pdfGenerator,
            mockk(),
            mockk(),
        )
    private val bruker = simpleSaksbehandler("Z123456")

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(
            db,
            sakOgBehandlingService,
            adresseService,
            journalfoerBrevService,
            distribusjonService,
            brevbaker,
        )
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
    inner class BrevUtgaar {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["FERDIGSTILT", "JOURNALFOERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Brev markeres som utgått - ugyldig status`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev

            assertThrows<UgyldigForespoerselException> {
                brevService.markerSomUtgaatt(brev.id, "kommentar", simpleSaksbehandler())
            }

            verify { db.hentBrev(brev.id) }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["FERDIGSTILT", "JOURNALFOERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Brev markeres som utgått - gyldig status, men for nytt`(status: Status) {
            val brev =
                opprettBrev(status, BrevProsessType.REDIGERBAR, opprettet = Tidspunkt.now().plus(6, ChronoUnit.DAYS))

            every { db.hentBrev(any()) } returns brev

            assertThrows<UgyldigForespoerselException> {
                brevService.markerSomUtgaatt(brev.id, "kommentar", simpleSaksbehandler())
            }

            verify { db.hentBrev(brev.id) }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["FERDIGSTILT", "JOURNALFOERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Brev kan markeres som utgått`(status: Status) {
            val saksbehandler = simpleSaksbehandler()
            val kommentar = "en liten kommentar"

            val brev =
                opprettBrev(
                    status,
                    BrevProsessType.REDIGERBAR,
                    opprettet = Tidspunkt.now().plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                )

            every { db.hentBrev(any()) } returns brev

            brevService.markerSomUtgaatt(brev.id, kommentar, saksbehandler)

            verify {
                db.hentBrev(brev.id)
                db.settBrevUtgaatt(brev.id, kommentar, saksbehandler)
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
        opprettet: Tidspunkt = Tidspunkt.now(),
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
        opprettet = opprettet,
        mottakere = listOf(opprettMottaker()),
        brevtype = Brevtype.INFORMASJON,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker() =
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
        )
}
