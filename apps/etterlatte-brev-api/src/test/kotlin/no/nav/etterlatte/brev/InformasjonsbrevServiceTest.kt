package no.nav.etterlatte.brev

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.AVVENTER_AVKLARING
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.FERDIG
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.JOURNALFOERING_PAABEGYNT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.OPPRETTET
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.LanguageCode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.db.InformasjonsbrevKonflikt
import no.nav.etterlatte.brev.db.InformasjonsbrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.brev.distribusjon.DistribusjonService
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.BrevbakerType.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.BrevbakerType.Telefonnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class InformasjonsbrevServiceTest(
    private val ds: DataSource,
) {
    private val db = BrevRepository(ds)
    private val repository = InformasjonsbrevRepository(ds, db)
    private val adresse = mockk<AdresseService>()
    private val brevbaker = mockk<BrevbakerService>()
    private val behandling = mockk<BehandlingService>()
    private val dokarkiv = mockk<DokarkivService>()
    private val distribusjon = mockk<DistribusjonService>()
    private val journalfoerer = JournalfoerBrevService(db, behandling, dokarkiv, mockk<VedtaksbrevService>())
    private val distribuerer = Brevdistribuerer(db, distribusjon)
    private val bruker = systembruker(mapOf(Claims.azp_name to "test:etterlatte:etterlatte-behandling"))
    private val config = BrevBatchKonfigurasjon(true, bruker.ident())
    private val sakId = SakId(123)
    private val request =
        StrukturertInformasjonsbrevRequest(
            sak = Sak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, sakId, Enhetsnummer("4815"), null, false),
            soeker = Soeker("Fiktiv", etternavn = "Person", fnr = Foedselsnummer(SOEKER_FOEDSELSNUMMER.value), under18 = false),
            innsender = null,
            avdoede = emptyList(),
            verge = null,
            gjenlevende = emptyList(),
            tittel = "Informasjon om saken",
            innhold = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = "Felles informasjon."))))),
            spraak = Spraak.NN,
        )
    private val mottaker =
        Mottaker(
            UUID.randomUUID(),
            "Fiktiv Person",
            MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
            null,
            Adresse(
                "NORSKPOSTADRESSE",
                adresselinje1 = "Testgata 1",
                postnummer = "0123",
                poststed = "Oslo",
                land = "Norge",
                landkode = "NOR",
            ),
        )
    private val service = nyService()

    private fun nyService() = InformasjonsbrevService(repository, db, adresse, brevbaker, journalfoerer, distribuerer, config)

    companion object {
        @RegisterExtension
        @JvmField
        val database = DatabaseExtension()
    }

    @BeforeEach
    fun setup() {
        coEvery { adresse.hentMottakere(any(), any(), any(), any(), true) } answers { listOf(mottaker.copy(id = UUID.randomUUID())) }
        coEvery { adresse.hentAvsender(any(), any()) } returns Avsender("Nav Fiktiv", Telefonnummer("5555"), null, null)
        coEvery { brevbaker.genererPdf(any(), any()) } returns Pdf("fiktiv pdf".toByteArray())
        coEvery { behandling.hentSak(any(), any()) } returns request.sak
        coEvery { dokarkiv.journalfoer(any(), any()) } returns OpprettJournalpostResponse("journal-123", true)
        every { distribusjon.distribuerJournalpost(any(), any(), any(), any()) } returns DistribuerJournalpostResponse("bestilling-123")
    }

    @AfterEach
    fun cleanup() = database.resetDb()

    @Test
    fun `samme utsending opprettes atomisk ved samtidige kall og innhold er uforanderlig`() {
        val id = UUID.randomUUID()
        val start = CountDownLatch(1)
        val kall =
            (1..8).map {
                CompletableFuture.supplyAsync {
                    start.await()
                    runBlocking { service.opprett(sakId, id, request, bruker) }
                }
            }
        start.countDown()
        val brevIder = kall.map { it.get(30, TimeUnit.SECONDS).brevId }.toSet()
        assertEquals(1, brevIder.size)
        assertEquals(1, db.hentBrevForSak(sakId).size)
        val brevId = brevIder.single()
        assertNull(db.hentBrev(brevId).behandlingId)
        assertEquals(Brevtype.INFORMASJON, db.hentBrev(brevId).brevtype)
        assertEquals(request.innhold, db.hentBrevPayload(brevId))
        assertEquals(request.tittel, db.hentBrevInnhold(brevId)!!.tittel)
        assertEquals(request.spraak, db.hentBrevInnhold(brevId)!!.spraak)
        assertThrows(InformasjonsbrevKonflikt::class.java) {
            runBlocking { service.opprett(sakId, id, request.copy(tittel = "Annen tittel"), bruker) }
        }
        assertThrows(InformasjonsbrevKonflikt::class.java) { service.hent(SakId(999), id, bruker) }
        assertThrows(Exception::class.java) { db.oppdaterTittel(brevId, "Endret", bruker) }
        assertThrows(Exception::class.java) { db.oppdaterSpraak(brevId, Spraak.NB, bruker) }
        assertThrows(Exception::class.java) { db.oppdaterPayload(brevId, request.innhold, bruker) }
        assertThrows(Exception::class.java) { db.oppdaterPayloadVedlegg(brevId, emptyList(), bruker) }
        assertThrows(Exception::class.java) { db.oppdaterBrevkoder(brevId, Brevkoder.TOMT_INFORMASJONSBREV) }
        assertThrows(Exception::class.java) { db.opprettMottaker(brevId, mottaker, bruker) }
        val mottakerId =
            db
                .hentBrev(brevId)
                .mottakere
                .single()
                .id
        assertThrows(Exception::class.java) { db.oppdaterMottaker(brevId, mottaker.copy(id = mottakerId, navn = "Endret navn"), bruker) }
        assertThrows(Exception::class.java) { db.slettMottaker(brevId, mottakerId, bruker) }
        assertThrows(Exception::class.java) { db.lagrePdf(brevId, Pdf("endret".toByteArray())) }
        assertThrows(Exception::class.java) { db.settBrevFerdigstilt(brevId, bruker) }
        assertThrows(UgyldigForespoerselException::class.java) { runBlocking { journalfoerer.journalfoer(brevId, bruker) } }
        assertThrows(UgyldigForespoerselException::class.java) { distribuerer.distribuer(brevId, bruker = bruker) }
    }

    @Test
    fun `PDF bruker eksakt tittel innhold spraak person og automatisk avsender og send er idempotent`() =
        runBlocking {
            val id = UUID.randomUUID()
            val opprettet = service.opprett(sakId, id, request, bruker)
            val pdfRequest = slot<BrevbakerRequest>()
            val avsenderRequest = slot<AvsenderRequest>()
            coEvery { adresse.hentAvsender(capture(avsenderRequest), bruker) } returns
                Avsender("Nav Fiktiv", Telefonnummer("5555"), null, null)
            coEvery { brevbaker.genererPdf(opprettet.brevId, capture(pdfRequest)) } returns Pdf("fiktiv pdf".toByteArray())
            assertEquals(FERDIG, service.send(sakId, id, bruker).status)
            assertEquals(FERDIG, nyService().send(sakId, id, bruker).status)
            assertEquals(Brevkoder.TOMT_INFORMASJONSBREV.ferdigstilling, pdfRequest.captured.kode)
            assertEquals(
                ManueltBrevMedTittelData(request.innhold.elements, ManueltBrevMedTittelDataData(request.tittel)),
                pdfRequest.captured.letterData,
            )
            assertEquals(LanguageCode.NYNORSK, pdfRequest.captured.language)
            assertEquals("EY", avsenderRequest.captured.saksbehandlerIdent)
            assertEquals(request.sak.enhet, avsenderRequest.captured.sakenhet)
            assertNull(avsenderRequest.captured.attestantIdent)
            assertNull(db.hentBrev(opprettet.brevId).behandlingId)
            coVerify(exactly = 1) { brevbaker.genererPdf(any(), any()) }
            coVerify(exactly = 1) { dokarkiv.journalfoer(any(), any()) }
            verify(exactly = 1) { distribusjon.distribuerJournalpost(any(), any(), any(), any()) }
        }

    @Test
    fun `uavklart journalfoering blir ikke proevd igjen etter omstart`() =
        runBlocking {
            val id = UUID.randomUUID()
            service.opprett(sakId, id, request, bruker)
            coEvery { dokarkiv.journalfoer(any(), any()) } throws IllegalStateException("timeout")
            assertEquals(AVVENTER_AVKLARING, service.send(sakId, id, bruker).status)
            assertEquals(AVVENTER_AVKLARING, nyService().send(sakId, id, bruker).status)
            coVerify(exactly = 1) { dokarkiv.journalfoer(any(), any()) }
            verify(exactly = 0) { distribusjon.distribuerJournalpost(any(), any(), any(), any()) }
        }

    @Test
    fun `uavklart distribusjon blir ikke proevd igjen etter omstart`() =
        runBlocking {
            val id = UUID.randomUUID()
            service.opprett(sakId, id, request, bruker)
            every { distribusjon.distribuerJournalpost(any(), any(), any(), any()) } throws IllegalStateException("timeout")
            assertEquals(AVVENTER_AVKLARING, service.send(sakId, id, bruker).status)
            assertEquals(AVVENTER_AVKLARING, nyService().send(sakId, id, bruker).status)
            coVerify(exactly = 1) { dokarkiv.journalfoer(any(), any()) }
            verify(exactly = 1) { distribusjon.distribuerJournalpost(any(), any(), any(), any()) }
        }

    @Test
    fun `samtidig send med utloept lease avklarer uten nytt kall og original eier kan fullfoere`() =
        runBlocking {
            val id = UUID.randomUUID()
            service.opprett(sakId, id, request, bruker)
            val paabegynt = CountDownLatch(1)
            val fortsett = CountDownLatch(1)
            coEvery { dokarkiv.journalfoer(any(), any()) } coAnswers {
                paabegynt.countDown()
                check(fortsett.await(30, TimeUnit.SECONDS))
                OpprettJournalpostResponse("journal-123", true)
            }
            val foerste = CompletableFuture.supplyAsync { runBlocking { service.send(sakId, id, bruker) } }
            try {
                assertTrue(paabegynt.await(30, TimeUnit.SECONDS))
                using(sessionOf(ds)) {
                    it.run(
                        queryOf(
                            "UPDATE informasjonsbrev_utsending SET laast_til = now() - interval '1 minute' WHERE utsendings_id = ?",
                            id,
                        ).asUpdate,
                    )
                }
                assertEquals(AVVENTER_AVKLARING, nyService().send(sakId, id, bruker).status)
                assertEquals(JOURNALFOERING_PAABEGYNT, repository.hent(id)!!.resultat.status)
            } finally {
                fortsett.countDown()
            }
            assertEquals(FERDIG, foerste.get(30, TimeUnit.SECONDS).status)
            coVerify(exactly = 1) { dokarkiv.journalfoer(any(), any()) }
            verify(exactly = 1) { distribusjon.distribuerJournalpost(any(), any(), any(), any()) }
        }

    @Test
    fun `feil foer sideeffekt kan trygt gjenopptas og lagret PDF blir gjenbrukt`() =
        runBlocking {
            val id = UUID.randomUUID()
            service.opprett(sakId, id, request, bruker)
            coEvery { brevbaker.genererPdf(any(), any()) } throws IllegalStateException("midlertidig")
            assertEquals(OPPRETTET, service.send(sakId, id, bruker).status)
            coEvery { brevbaker.genererPdf(any(), any()) } returns Pdf("fiktiv pdf".toByteArray())
            assertEquals(FERDIG, nyService().send(sakId, id, bruker).status)
            coVerify(exactly = 1) { dokarkiv.journalfoer(any(), any()) }
        }

    @Test
    fun `ukjent mottaker ingen tilganger og stoppbryter blokkerer opprettelse`() {
        coEvery { adresse.hentMottakere(any(), any(), any(), any(), true) } returns
            listOf(mottaker.copy(adresse = Adresse("", landkode = "", land = "")))
        assertThrows(UgyldigForespoerselException::class.java) {
            runBlocking { service.opprett(sakId, UUID.randomUUID(), request, bruker) }
        }
        assertTrue(db.hentBrevForSak(sakId).isEmpty())
        assertThrows(ForespoerselException::class.java) { config.kontrollerTilgang(simpleSaksbehandler(), true) }
        assertThrows(ForespoerselException::class.java) { BrevBatchKonfigurasjon().kontrollerTilgang(bruker, false) }
        assertThrows(ForespoerselException::class.java) { BrevBatchKonfigurasjon(false, bruker.ident()).kontrollerTilgang(bruker, true) }
        assertDoesNotThrow { BrevBatchKonfigurasjon(false, bruker.ident()).kontrollerTilgang(bruker, false) }
    }
}
