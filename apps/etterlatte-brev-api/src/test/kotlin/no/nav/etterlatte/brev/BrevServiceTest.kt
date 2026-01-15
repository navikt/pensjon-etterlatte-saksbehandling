package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
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
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.PdfMedData
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.pdf.PDFService
import no.nav.etterlatte.brev.vedtaksbrev.UgyldigAntallMottakere
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.VERGE_FOEDSELSNUMMER
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
    private val pdfService = mockk<PDFService>()
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
            pdfService,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
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
            brevService.oppdaterTittel(brev.id, tittel, bruker)

            verify {
                db.hentBrev(brev.id)
                db.oppdaterTittel(brev.id, tittel, bruker)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Oppdater tittel - kan IKKE endres`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                brevService.oppdaterTittel(brev.id, "Ny tittel skal feile", bruker)
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
                opprettBrev(status, BrevProsessType.REDIGERBAR, opprettet = Tidspunkt.now().minus(6, ChronoUnit.DAYS))

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
                    opprettet = Tidspunkt.now().minus(7, ChronoUnit.DAYS),
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
    inner class OpprettMottakerTest {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Opprett mottaker på redigerbart brev`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev

            val faktiskMottaker = brevService.opprettMottaker(brev.id, bruker)

            faktiskMottaker.type shouldBe MottakerType.KOPI

            verify {
                db.hentBrev(brev.id)
                db.opprettMottaker(brev.id, match { it.type == MottakerType.KOPI }, bruker)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Opprett mottaker på redigerbart brev, men maks antall er nådd`(status: Status) {
            val brev =
                opprettBrev(
                    status,
                    BrevProsessType.REDIGERBAR,
                    mottakere = listOf(opprettMottaker(), opprettMottaker()), // Maks to mottakere
                )

            every { db.hentBrev(any()) } returns brev

            assertThrows<MaksAntallMottakere> { brevService.opprettMottaker(brev.id, bruker) }

            verify {
                db.hentBrev(brev.id)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Opprett mottaker på brev som ikke kan endres`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                brevService.opprettMottaker(brev.id, bruker)
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    @Nested
    inner class OppdaterMottakerTest {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Oppdater mottaker på redigerbart brev`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev

            val nyMottaker =
                Mottaker(
                    id = brev.mottakere.single().id,
                    navn = "NYTT NAVN",
                    foedselsnummer = MottakerFoedselsnummer(VERGE_FOEDSELSNUMMER.value),
                    adresse =
                        Adresse(
                            adresseType = "UTENLANDSKPOSTADRESSE",
                            adresselinje1 = "NY ADRESSELINJE",
                            postnummer = "",
                            poststed = "",
                            landkode = "SE",
                            land = "SVERIGE",
                        ),
                )

            brevService.oppdaterMottaker(brev.id, nyMottaker, bruker)

            verify {
                db.hentBrev(brev.id)
                db.oppdaterMottaker(brev.id, nyMottaker, bruker)
            }
        }

        @Test
        fun `Validere postnummer og poststed paa utenlandskpostadresse`() {
            val brev = opprettBrev(Status.OPPDATERT, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev

            val nyMottaker =
                Mottaker(
                    id = brev.mottakere.single().id,
                    navn = "NYTT NAVN",
                    foedselsnummer = MottakerFoedselsnummer(VERGE_FOEDSELSNUMMER.value),
                    adresse =
                        Adresse(
                            adresseType = "UTENLANDSKPOSTADRESSE",
                            adresselinje1 = "NY ADRESSELINJE",
                            postnummer = "6899",
                            poststed = "DANMARK",
                            landkode = "SE",
                            land = "SVERIGE",
                        ),
                )

            val exception =
                assertThrows<UgyldigForespoerselException> {
                    brevService.oppdaterMottaker(brev.id, nyMottaker, bruker)
                }

            exception.message shouldBe "Postnummer og poststed skal ikke brukes på utenlandsk adresse"

            verify {
                db.hentBrev(brev.id)
            }
            verify(exactly = 0) {
                db.oppdaterMottaker(brev.id, nyMottaker, bruker)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Oppdater mottaker på brev som ikke kan endres`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)
            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                brevService.oppdaterMottaker(brev.id, tomMottaker(type = MottakerType.HOVED), bruker)
            }

            verify {
                db.hentBrev(brev.id)
            }
        }

        @Test
        fun `Sett ny hovedmottaker på brev - kopimottaker blir hovedmottaker`() {
            val hovedmottaker = opprettMottaker(type = MottakerType.HOVED)
            val kopimottaker = opprettMottaker(type = MottakerType.KOPI)

            val brev =
                opprettBrev(
                    status = Status.OPPDATERT,
                    prosessType = BrevProsessType.REDIGERBAR,
                    mottakere = listOf(hovedmottaker, kopimottaker),
                )

            every { db.hentBrev(any()) } returns brev

            brevService.settHovedmottaker(brev.id, kopimottaker.id, bruker)

            verify {
                db.hentBrev(brev.id)

                db.oppdaterMottaker(
                    brev.id,
                    match { it.id == hovedmottaker.id && it.type == MottakerType.KOPI },
                    bruker,
                )

                db.oppdaterMottaker(
                    brev.id,
                    match { it.id == kopimottaker.id && it.type == MottakerType.HOVED },
                    bruker,
                )
            }
        }

        @Test
        fun `Sett hovedmottaker som hovedmottaker gjør ingenting`() {
            val hovedmottaker = opprettMottaker(type = MottakerType.HOVED)
            val kopimottaker = opprettMottaker(type = MottakerType.KOPI)

            val brev =
                opprettBrev(
                    status = Status.OPPDATERT,
                    prosessType = BrevProsessType.REDIGERBAR,
                    mottakere = listOf(hovedmottaker, kopimottaker),
                )

            every { db.hentBrev(any()) } returns brev

            brevService.settHovedmottaker(brev.id, hovedmottaker.id, bruker)

            verify { db.hentBrev(brev.id) }
            verify(exactly = 0) { db.oppdaterMottaker(any(), any(), any()) }
        }
    }

    @Nested
    inner class SlettMottakerTest {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Slett KOPI-mottaker på redigerbart brev`(status: Status) {
            val hovedmottaker = opprettMottaker(type = MottakerType.HOVED)
            val kopimottaker = opprettMottaker(type = MottakerType.KOPI)

            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR, mottakere = listOf(hovedmottaker, kopimottaker))

            every { db.hentBrev(any()) } returns brev

            brevService.slettMottaker(brev.id, kopimottaker.id, bruker)

            verify {
                db.hentBrev(brev.id)
                db.slettMottaker(brev.id, kopimottaker.id, bruker)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Slett HOVED-mottaker på redigerbart brev`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)
            val mottaker = brev.mottakere.single()

            every { db.hentBrev(any()) } returns brev

            assertThrows<KanIkkeSletteHovedmottaker> { brevService.slettMottaker(brev.id, mottaker.id, bruker) }

            verify {
                db.hentBrev(brev.id)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.EXCLUDE)
        fun `Sletting av mottaker på brev som ikke kan endres`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                brevService.slettMottaker(brev.id, mockk(), bruker)
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    @Nested
    inner class FerdigstillingAvBrev {
        @ParameterizedTest
        @EnumSource(Status::class)
        fun `Skal ikke kunne ferdigstille aktivitetspliktsbrev som medfører vurdering, uavhengig av status`(status: Status) {
            val brev =
                opprettBrev(
                    status,
                    BrevProsessType.REDIGERBAR,
                ).copy(brevkoder = Brevkoder.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_10MND_INNHOLD)

            every { db.hentBrev(any()) } returns brev

            assertThrows<BrevKanIkkeEndres> {
                runBlocking { brevService.ferdigstill(brev.id, bruker) }
            }

            verify {
                db.hentBrev(brev.id)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Kan ferdigstille brev med en hovedmottaker`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

            every { db.hentBrev(any()) } returns brev
            val pdf = Pdf(bytes = ByteArray(1))
            coEvery { pdfService.genererPdf(brev.id, any(), any(), any(), any()) } returns pdf
            runBlocking { brevService.ferdigstill(brev.id, bruker) }

            verify {
                db.hentBrev(brev.id)
                db.lagrePdfOgFerdigstillBrev(brev.id, pdf, any())
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"], mode = EnumSource.Mode.INCLUDE)
        fun `Kan ikke ferdigstille brev med mer enn 2 mottakere`(status: Status) {
            val brev =
                opprettBrev(status, BrevProsessType.REDIGERBAR)
                    .copy(mottakere = listOf(opprettMottaker(), opprettMottaker(), opprettMottaker()))

            every { db.hentBrev(any()) } returns brev
            val pdf = Pdf(bytes = ByteArray(1))
            coEvery { pdfService.genererPdf(brev.id, any(), any(), any(), any()) } returns pdf
            assertThrows<UgyldigAntallMottakere> {
                runBlocking { brevService.ferdigstill(brev.id, bruker) }
            }

            verify {
                db.hentBrev(brev.id)
            }
        }
    }

    @Nested
    inner class SlettingAvBrev {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT", "SLETTET"], mode = EnumSource.Mode.INCLUDE)
        fun `Sletting av brev som er under arbeid, eller allerede er slettet skal virke`(status: Status) {
            val brev = opprettBrev(status, BrevProsessType.MANUELL)

            every { db.hentBrev(any()) } returns brev

            brevService.slett(brev.id, bruker)

            verify {
                db.hentBrev(brev.id)
                db.settBrevSlettet(brev.id, bruker)
            }
        }

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT", "SLETTET"], mode = EnumSource.Mode.EXCLUDE)
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
        mottakere: List<Mottaker> = listOf(opprettMottaker()),
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
        mottakere = mottakere,
        brevtype = Brevtype.INFORMASJON,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker(type: MottakerType = MottakerType.HOVED) =
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
            type = type,
        )
}
