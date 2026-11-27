package no.nav.etterlatte.brev

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.AVVENTER_AVKLARING
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.DISTRIBUSJON_PAABEGYNT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.FERDIG
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.FERDIGSTILT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.JOURNALFOERING_PAABEGYNT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.JOURNALFOERT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.OPPRETTET
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.PDF_LAGRET
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.LanguageCode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.db.InformasjonsbrevRepository
import no.nav.etterlatte.brev.db.LagretInformasjonsbrev
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.BrevbakerType.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.BrevbakerType.Telefonnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

class InformasjonsbrevSendTest {
    private val repository = mockk<InformasjonsbrevRepository>(relaxed = true)
    private val db = mockk<BrevRepository>()
    private val adresse = mockk<AdresseService>()
    private val brevbaker = mockk<BrevbakerService>()
    private val journal = mockk<JournalfoerBrevService>()
    private val distribusjon = mockk<Brevdistribuerer>()
    private val bruker = systembruker()
    private val id = UUID.randomUUID()
    private val sakId = SakId(123)
    private var status = FERDIGSTILT
    private var eier: UUID? = null
    private val request =
        StrukturertInformasjonsbrevRequest(
            Sak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, sakId, Enhetsnummer("4815"), null, false),
            Soeker("Fiktiv", etternavn = "Person", fnr = Foedselsnummer(SOEKER_FOEDSELSNUMMER.value), under18 = false),
            null,
            emptyList(),
            null,
            emptyList(),
            "Felles tittel",
            Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = "Felles informasjon."))))),
            Spraak.NN,
        )
    private val service =
        InformasjonsbrevService(repository, db, adresse, brevbaker, journal, distribusjon, BrevBatchKonfigurasjon(true, bruker.ident()))

    @BeforeEach
    fun setup() {
        every { repository.hent(id) } answers {
            LagretInformasjonsbrev(InformasjonsbrevUtsending(id, sakId, 1, status), request, bruker.ident(), "fingerprint", eier)
        }
        every { repository.kontroller(any(), sakId, bruker.ident(), any()) } answers { firstArg() }
        every { repository.overta(id, any()) } answers {
            if (status in setOf(OPPRETTET, PDF_LAGRET, FERDIGSTILT, JOURNALFOERT)) {
                eier = secondArg()
                true
            } else {
                false
            }
        }
        every { repository.steg(id, any(), any(), any(), any()) } answers {
            if (eier == secondArg<UUID>()) {
                status = thirdArg()
                if (arg<Boolean>(4)) eier = null
                true
            } else {
                false
            }
        }
        every { repository.ferdigstill(id, any(), bruker) } answers { status = FERDIGSTILT }
        val mottaker = mockk<Mottaker>()
        every { mottaker.journalpostId } returns "journal-123"
        every { mottaker.bestillingId } returns "bestilling-123"
        val brev = mockk<Brev>()
        every { brev.mottakere } returns listOf(mottaker)
        every { db.hentBrev(1) } returns brev
        coEvery { journal.journalfoer(1, bruker, any()) } returns emptyList()
        every { distribusjon.distribuer(1, any(), bruker, any()) } returns listOf("bestilling-123")
    }

    @Test
    fun `eksterne sideeffekter blir markert paabegynt foer kall og ferdig foerst etter alle IDer`() =
        runBlocking {
            coEvery { journal.journalfoer(1, bruker, any()) } coAnswers {
                assertEquals(JOURNALFOERING_PAABEGYNT, status)
                assertEquals(AVVENTER_AVKLARING, service.hent(sakId, id, bruker)!!.status)
                assertEquals(JOURNALFOERING_PAABEGYNT, status)
                emptyList()
            }
            every { distribusjon.distribuer(1, any(), bruker, any()) } answers {
                assertEquals(DISTRIBUSJON_PAABEGYNT, status)
                listOf("bestilling-123")
            }
            assertEquals(FERDIG, service.send(sakId, id, bruker).status)
            verifyOrder {
                repository.steg(id, any(), JOURNALFOERING_PAABEGYNT, null, false)
                repository.steg(id, any(), JOURNALFOERT, null, false)
                repository.steg(id, any(), DISTRIBUSJON_PAABEGYNT, null, false)
                repository.steg(id, any(), FERDIG, null, true)
            }
        }

    @ParameterizedTest
    @EnumSource(
        value = InformasjonsbrevUtsendingStatus::class,
        names = ["JOURNALFOERING_PAABEGYNT", "DISTRIBUSJON_PAABEGYNT", "AVVENTER_AVKLARING", "FERDIG", "FEILA"],
    )
    fun `påbegynt uavklart ferdig eller feilet utsending får aldri nye sideeffekter`(lagretStatus: InformasjonsbrevUtsendingStatus) =
        runBlocking {
            status = lagretStatus
            val resultat = service.send(sakId, id, bruker)
            assertEquals(
                if (status in
                    setOf(JOURNALFOERING_PAABEGYNT, DISTRIBUSJON_PAABEGYNT)
                ) {
                    AVVENTER_AVKLARING
                } else {
                    status
                },
                resultat.status,
            )
            coVerify(exactly = 0) { journal.journalfoer(any(), any(), any()) }
            verify(exactly = 0) { distribusjon.distribuer(any(), any(), any(), any()) }
            verify(exactly = 0) { repository.steg(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `journalføringsfeil er uavklart og blir ikke automatisk forsøkt igjen`() =
        runBlocking {
            coEvery { journal.journalfoer(1, bruker, any()) } throws IllegalStateException("timeout")
            assertEquals(AVVENTER_AVKLARING, service.send(sakId, id, bruker).status)
            assertEquals(AVVENTER_AVKLARING, service.send(sakId, id, bruker).status)
            coVerify(exactly = 1) { journal.journalfoer(any(), any(), any()) }
            verify(exactly = 0) { distribusjon.distribuer(any(), any(), any(), any()) }
        }

    @Test
    fun `distribusjonsfeil er uavklart og blir ikke automatisk forsøkt igjen`() =
        runBlocking {
            every { distribusjon.distribuer(1, any(), bruker, any()) } throws IllegalStateException("timeout")
            assertEquals(AVVENTER_AVKLARING, service.send(sakId, id, bruker).status)
            assertEquals(AVVENTER_AVKLARING, service.send(sakId, id, bruker).status)
            verify(exactly = 1) { distribusjon.distribuer(any(), any(), any(), any()) }
        }

    @Test
    fun `fullført journalføring gjenopptar bare distribusjon`() =
        runBlocking {
            status = JOURNALFOERT
            assertEquals(FERDIG, service.send(sakId, id, bruker).status)
            coVerify(exactly = 0) { journal.journalfoer(any(), any(), any()) }
            verify(exactly = 1) { distribusjon.distribuer(any(), any(), any(), any()) }
        }

    @Test
    fun `lagret PDF gjenopptas uten ny rendering`() =
        runBlocking {
            status = PDF_LAGRET
            assertEquals(FERDIG, service.send(sakId, id, bruker).status)
            verify(exactly = 1) { repository.ferdigstill(id, any(), bruker) }
            coVerify(exactly = 0) { brevbaker.genererPdf(any(), any()) }
        }

    @Test
    fun `PDF sender eksakt innhold språk person og EY til generisk strukturert mal`() =
        runBlocking {
            status = OPPRETTET
            val pdfRequest = slot<BrevbakerRequest>()
            val avsenderRequest = slot<AvsenderRequest>()
            coEvery { adresse.hentAvsender(capture(avsenderRequest), bruker) } returns
                Avsender("Nav Fiktiv", Telefonnummer("5555"), null, null)
            coEvery { brevbaker.genererPdf(1, capture(pdfRequest)) } returns Pdf("fiktiv PDF".toByteArray())
            every { repository.lagrePdf(id, any(), any()) } answers { status = PDF_LAGRET }
            assertEquals(FERDIG, service.send(sakId, id, bruker).status)
            assertEquals(
                ManueltBrevMedTittelData(request.innhold.elements, ManueltBrevMedTittelDataData(request.tittel)),
                pdfRequest.captured.letterData,
            )
            assertEquals(Brevkoder.TOMT_INFORMASJONSBREV.ferdigstilling, pdfRequest.captured.kode)
            assertEquals(LanguageCode.NYNORSK, pdfRequest.captured.language)
            assertEquals(request.soeker.fnr, pdfRequest.captured.felles.bruker.foedselsnummer)
            assertEquals(request.soeker.fornavn, pdfRequest.captured.felles.bruker.fornavn)
            assertNull(pdfRequest.captured.felles.signerendeSaksbehandlere)
            assertEquals("EY", avsenderRequest.captured.saksbehandlerIdent)
            assertEquals(request.sak.enhet, avsenderRequest.captured.sakenhet)
            assertNull(avsenderRequest.captured.attestantIdent)
        }

    @Test
    fun `PDF feil kan trygt forsøkes igjen før eksterne sideeffekter`() =
        runBlocking {
            status = OPPRETTET
            coEvery { adresse.hentAvsender(any(), bruker) } returns Avsender("Nav Fiktiv", Telefonnummer("5555"), null, null)
            coEvery { brevbaker.genererPdf(any(), any()) } throws IllegalStateException("midlertidig")
            assertEquals(OPPRETTET, service.send(sakId, id, bruker).status)
            coEvery { brevbaker.genererPdf(any(), any()) } returns Pdf("fiktiv PDF".toByteArray())
            every { repository.lagrePdf(id, any(), any()) } answers { status = PDF_LAGRET }
            assertEquals(FERDIG, service.send(sakId, id, bruker).status)
            coVerify(exactly = 1) { journal.journalfoer(any(), any(), any()) }
        }
}
