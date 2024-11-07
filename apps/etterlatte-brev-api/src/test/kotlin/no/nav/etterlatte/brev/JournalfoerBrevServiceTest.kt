package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.JournalPostType
import no.nav.etterlatte.brev.dokarkiv.JournalpostKoder
import no.nav.etterlatte.brev.dokarkiv.JournalpostRequest
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.VERGE_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JournalfoerBrevServiceTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val dokarkivService = mockk<DokarkivService>()
    private val vedtaksbrevService = mockk<VedtaksbrevService>()
    private val bruker = simpleSaksbehandler("Z123456")

    private val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)

    private companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, behandlingService, dokarkivService, vedtaksbrevService)
    }

    @Test
    fun `Journalfoering fungerer som forventet (kun 1 mottaker)`() {
        val brev = opprettBrev(Status.FERDIGSTILT)
        val sak = Sak("ident", SakType.BARNEPENSJON, brev.sakId, Enheter.defaultEnhet.enhetNr)
        val journalpostResponse = OpprettJournalpostResponse("444", journalpostferdigstilt = true)

        coEvery { behandlingService.hentSak(any(), any()) } returns sak
        coEvery { dokarkivService.journalfoer(any(), any()) } returns journalpostResponse
        every { db.hentBrev(any()) } returns brev
        every { db.settBrevJournalfoert(any(), any(), any()) } returns true

        val faktiskJournalpostId =
            runBlocking {
                service.journalfoer(brev.id, bruker)
            }

        faktiskJournalpostId shouldBe listOf(journalpostResponse)

        verify {
            db.hentBrev(brev.id)
            db.hentBrevInnhold(brev.id)
            db.hentPdf(brev.id)
            db.lagreJournalpostId(brev.mottakere.single().id, journalpostResponse)
            db.settBrevJournalfoert(brev.id, listOf(journalpostResponse), bruker)
        }
        coVerify {
            behandlingService.hentSak(sak.id, bruker)
            dokarkivService.journalfoer(any(), any())
        }
    }

    @Test
    fun `Journalfoering fungerer som forventet (flere mottakere)`() {
        val mottaker1 = opprettMottaker(SOEKER_FOEDSELSNUMMER.value)
        val mottaker2 = opprettMottaker(VERGE_FOEDSELSNUMMER.value)

        val brev =
            opprettBrev(
                status = Status.FERDIGSTILT,
                mottakere = listOf(mottaker1, mottaker2),
            )

        val sak = Sak("ident", SakType.BARNEPENSJON, brev.sakId, Enheter.defaultEnhet.enhetNr)
        val response1 = OpprettJournalpostResponse(Random.nextLong().toString(), journalpostferdigstilt = true)
        val response2 = OpprettJournalpostResponse(Random.nextLong().toString(), journalpostferdigstilt = true)

        coEvery { behandlingService.hentSak(any(), any()) } returns sak
        coEvery { dokarkivService.journalfoer(any(), any()) } returns response1 andThen response2

        every { db.hentBrev(any()) } returns brev
        every { db.settBrevJournalfoert(any(), any(), any()) } returns true

        val faktiskResponse =
            runBlocking {
                service.journalfoer(brev.id, bruker)
            }

        faktiskResponse shouldBe listOf(response1, response2)

        verify {
            db.hentBrev(brev.id)
            db.hentBrevInnhold(brev.id)
            db.hentPdf(brev.id)

            db.lagreJournalpostId(brev.mottakere[0].id, response1)
            db.lagreJournalpostId(brev.mottakere[1].id, response2)

            db.settBrevJournalfoert(brev.id, listOf(response1, response2), bruker)
        }
        coVerify {
            behandlingService.hentSak(sak.id, bruker)
            dokarkivService.journalfoer(any(), any())
        }
    }

    @ParameterizedTest
    @EnumSource(
        Status::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["FERDIGSTILT"],
    )
    fun `Journalfoering feiler hvis status er feil`(status: Status) {
        val brev = opprettBrev(status)
        every { db.hentBrev(any()) } returns brev
        coEvery {
            behandlingService.hentSak(
                any(),
                any(),
            )
        } returns Sak(brev.soekerFnr, SakType.BARNEPENSJON, randomSakId(), Enheter.UTLAND.enhetNr)

        runBlocking {
            assertThrows<FeilStatusForJournalfoering> {
                service.journalfoer(brev.id, bruker)
            }
        }

        verify(exactly = 1) {
            db.hentBrev(brev.id)
        }
        coVerify(exactly = 1) {
            behandlingService.hentSak(brev.sakId, bruker)
        }
    }

    @Test
    fun `Brev finnes ikke for behandling`() {
        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns null

        val vedtak = opprettVedtak()

        assertThrows<NoSuchElementException> {
            runBlocking { service.journalfoerVedtaksbrev(vedtak, systembruker()) }
        }

        verify { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
    }

    @Test
    fun `Brev er allerede journalfoert`() {
        val brev =
            Brev(
                1,
                randomSakId(),
                BEHANDLING_ID,
                "tittel",
                spraak = Spraak.NB,
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.JOURNALFOERT,
                Tidspunkt.now(),
                Tidspunkt.now(),
                mottakere = mockk(),
                brevtype = Brevtype.MANUELT,
                brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
            )

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev

        val vedtak = opprettVedtak()

        runBlocking { service.journalfoerVedtaksbrev(vedtak, systembruker()) }

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
        coVerify(exactly = 0) { dokarkivService.journalfoer(any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Journalfoeringsrequest for vedtaksbrev mappes korrekt`(type: SakType) {
        val behandlingId = UUID.randomUUID()
        val forventetBrevMottakerFnr = SOEKER_FOEDSELSNUMMER.value
        val systembruker = systembruker()

        val sak = Sak(forventetBrevMottakerFnr, type, randomSakId(), Enheter.defaultEnhet.enhetNr)

        val forventetBrev =
            Brev(
                id = 123,
                sakId = sak.id,
                behandlingId = behandlingId,
                tittel = null,
                spraak = Spraak.NB,
                prosessType = BrevProsessType.AUTOMATISK,
                soekerFnr = forventetBrevMottakerFnr,
                status = Status.FERDIGSTILT,
                statusEndret = Tidspunkt.now(),
                opprettet = Tidspunkt.now(),
                mottakere = listOf(opprettMottaker(forventetBrevMottakerFnr)),
                brevtype = Brevtype.INFORMASJON,
                brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
            )

        coEvery { behandlingService.hentSak(forventetBrev.sakId, any()) } returns sak
        coEvery { vedtaksbrevService.hentVedtaksbrev(any()) } returns forventetBrev
        every { db.hentBrev(any()) } returns forventetBrev

        val innhold = BrevInnhold("tittel", Spraak.NB, payload = Slate())
        every { db.hentBrevInnhold(any()) } returns innhold

        val vedtak =
            VedtakTilJournalfoering(
                1,
                VedtakSak("ident", type, forventetBrev.sakId),
                behandlingId,
                sak.enhet,
                "EY",
            )

        val journalpostResponse =
            OpprettJournalpostResponse(
                journalpostId = Random.nextLong().toString(),
                journalpostferdigstilt = true,
            )
        coEvery { dokarkivService.journalfoer(any(), any()) } returns journalpostResponse

        runBlocking { service.journalfoerVedtaksbrev(vedtak, systembruker) }

        verify(exactly = 1) {
            db.hentBrevInnhold(forventetBrev.id)
            db.hentPdf(forventetBrev.id)
            db.lagreJournalpostId(forventetBrev.mottakere.single().id, journalpostResponse)
            db.settBrevJournalfoert(forventetBrev.id, listOf(journalpostResponse), systembruker)
        }

        val requestSlot = slot<JournalpostRequest>()
        coVerify(exactly = 1) {
            vedtaksbrevService.hentVedtaksbrev(forventetBrev.behandlingId!!)
            behandlingService.hentSak(forventetBrev.sakId, any())
            dokarkivService.journalfoer(capture(requestSlot), any())
        }

        with(requestSlot.captured) {
            this.tittel shouldBe innhold.tittel
            this.journalposttype shouldBe JournalPostType.UTGAAENDE
            this.avsenderMottaker.id shouldBe forventetBrev.soekerFnr
            this.bruker.id shouldBe forventetBrev.soekerFnr
            this.bruker.idType shouldBe BrukerIdType.FNR
            this.eksternReferanseId shouldContain forventetBrev.sakId.toString()
            this.eksternReferanseId shouldContain forventetBrev.id.toString()
            this.sak.sakstype shouldBe Sakstype.FAGSAK
            this.sak.tema shouldBe sak.sakType.tema
            this.sak.fagsakId shouldBe sak.id.toString()
            this.sak.fagsaksystem shouldBe Fagsaksystem.EY.navn
            this.tema shouldBe sak.sakType.tema
            this.kanal shouldBe "S"
            this.journalfoerendeEnhet shouldBe sak.enhet

            val dokument = this.dokumenter.single()
            dokument.tittel shouldBe innhold.tittel
            dokument.brevkode shouldBe JournalpostKoder.BREV_KODE
            dokument.dokumentvarianter.size shouldBe 1
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Journalfoeringsrequest for informasjonsbrev mappes korrekt`(type: SakType) {
        val forventetBrevMottakerFnr = SOEKER_FOEDSELSNUMMER.value
        val forventetBrev =
            Brev(
                id = 123,
                sakId = randomSakId(),
                behandlingId = null,
                tittel = null,
                spraak = Spraak.NB,
                prosessType = BrevProsessType.AUTOMATISK,
                soekerFnr = forventetBrevMottakerFnr,
                status = Status.FERDIGSTILT,
                statusEndret = Tidspunkt.now(),
                opprettet = Tidspunkt.now(),
                mottakere = listOf(opprettMottaker(forventetBrevMottakerFnr)),
                brevtype = Brevtype.MANUELT,
                brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
            )

        every { db.hentBrev(any()) } returns forventetBrev

        val innhold = BrevInnhold("tittel", Spraak.NB, payload = Slate())
        every { db.hentBrevInnhold(any()) } returns innhold

        val journalpostResponse =
            OpprettJournalpostResponse(
                journalpostId = Random.nextLong().toString(),
                journalpostferdigstilt = true,
            )
        coEvery { dokarkivService.journalfoer(any(), any()) } returns journalpostResponse

        val sak = Sak(forventetBrev.soekerFnr, type, forventetBrev.sakId, Enheter.PORSGRUNN.enhetNr)
        coEvery { behandlingService.hentSak(any(), any()) } returns sak

        runBlocking { service.journalfoer(forventetBrev.id, bruker) }

        verify(exactly = 1) {
            db.hentBrevInnhold(forventetBrev.id)
            db.hentPdf(forventetBrev.id)
            db.hentBrev(forventetBrev.id)
            db.lagreJournalpostId(forventetBrev.mottakere.single().id, journalpostResponse)
            db.settBrevJournalfoert(forventetBrev.id, listOf(journalpostResponse), bruker)
        }

        val requestSlot = slot<JournalpostRequest>()
        coVerify {
            behandlingService.hentSak(forventetBrev.sakId, bruker)
            dokarkivService.journalfoer(capture(requestSlot), any())
        }

        with(requestSlot.captured) {
            this.tittel shouldBe innhold.tittel
            this.journalposttype shouldBe JournalPostType.UTGAAENDE
            this.avsenderMottaker.id shouldBe forventetBrev.soekerFnr
            this.bruker.id shouldBe forventetBrev.soekerFnr
            this.bruker.idType shouldBe BrukerIdType.FNR
            this.eksternReferanseId shouldContain forventetBrev.sakId.toString()
            this.eksternReferanseId shouldContain forventetBrev.id.toString()
            this.sak.sakstype shouldBe Sakstype.FAGSAK
            this.sak.tema shouldBe sak.sakType.tema
            this.sak.fagsakId shouldBe sak.id.toString()
            this.sak.fagsaksystem shouldBe Fagsaksystem.EY.navn
            this.tema shouldBe sak.sakType.tema
            this.kanal shouldBe "S"
            this.journalfoerendeEnhet shouldBe sak.enhet

            val dokument = this.dokumenter.single()
            dokument.tittel shouldBe innhold.tittel
            dokument.brevkode shouldBe JournalpostKoder.BREV_KODE
            dokument.dokumentvarianter.size shouldBe 1
        }
    }

    private fun opprettBrev(
        status: Status,
        mottakere: List<Mottaker> = listOf(opprettMottaker(SOEKER_FOEDSELSNUMMER.value)),
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = randomSakId(),
        behandlingId = null,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = BrevProsessType.REDIGERBAR,
        soekerFnr = "fnr",
        status = status,
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        mottakere = mottakere,
        brevtype = Brevtype.INFORMASJON,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker(fnr: String) =
        Mottaker(
            id = UUID.randomUUID(),
            navn = "Stor Snerk",
            foedselsnummer = MottakerFoedselsnummer(fnr),
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

    private fun opprettVedtak(): VedtakTilJournalfoering =
        VedtakTilJournalfoering(
            vedtakId = 1L,
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, sakId2),
            behandlingId = UUID.randomUUID(),
            ansvarligEnhet = Enheter.defaultEnhet.enhetNr,
            saksbehandler = "EY",
        )
}
