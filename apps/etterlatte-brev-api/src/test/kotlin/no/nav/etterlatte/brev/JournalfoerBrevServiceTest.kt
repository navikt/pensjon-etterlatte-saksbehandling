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
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.JournalPostType
import no.nav.etterlatte.brev.dokarkiv.JournalpostKoder
import no.nav.etterlatte.brev.dokarkiv.JournalpostRequest
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
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
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random

class JournalfoerBrevServiceTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val dokarkivService = mockk<DokarkivService>()
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val bruker = simpleSaksbehandler("Z123456")

    @AfterEach
    fun after() {
        confirmVerified(db, behandlingService, dokarkivService, vedtaksbrevService)
        clearAllMocks()
    }

    @Test
    fun `Journalfoering fungerer som forventet`() {
        val brev = opprettBrev(Status.FERDIGSTILT, BrevProsessType.MANUELL)
        val sak = Sak("ident", SakType.BARNEPENSJON, brev.sakId, "1234")
        val journalpostResponse = OpprettJournalpostResponse("444", journalpostferdigstilt = true)

        val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)

        coEvery { behandlingService.hentSak(any(), any()) } returns sak
        coEvery { dokarkivService.journalfoer(any()) } returns journalpostResponse
        every { db.hentBrev(any()) } returns brev
        every { db.settBrevJournalfoert(any(), any()) } returns true

        val faktiskJournalpostId =
            runBlocking {
                service.journalfoer(brev.id, bruker)
            }

        faktiskJournalpostId shouldBe journalpostResponse.journalpostId

        verify {
            db.hentBrev(brev.id)
            db.hentBrevInnhold(brev.id)
            db.hentPdf(brev.id)
            db.settBrevJournalfoert(brev.id, journalpostResponse)
        }
        coVerify {
            behandlingService.hentSak(sak.id, bruker)
            dokarkivService.journalfoer(any())
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
        coEvery {
            behandlingService.hentSak(
                any(),
                any(),
            )
        } returns Sak(brev.soekerFnr, SakType.BARNEPENSJON, Random.nextLong(), Enheter.UTLAND.enhetNr)

        val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)

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

        val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)
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
                41,
                BEHANDLING_ID,
                "tittel",
                spraak = Spraak.NB,
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.JOURNALFOERT,
                Tidspunkt.now(),
                Tidspunkt.now(),
                mottaker = mockk(),
                brevtype = Brevtype.MANUELT,
                brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
            )

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev

        val vedtak = opprettVedtak()

        val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)
        runBlocking { service.journalfoerVedtaksbrev(vedtak, systembruker()) }

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
        coVerify(exactly = 0) { dokarkivService.journalfoer(any()) }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Journalfoeringsrequest for vedtaksbrev mappes korrekt`(type: SakType) {
        val behandlingId = UUID.randomUUID()
        val forventetBrevMottakerFnr = SOEKER_FOEDSELSNUMMER.value

        val sak = Sak(forventetBrevMottakerFnr, type, Random.nextLong(), Enheter.defaultEnhet.enhetNr)

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
                mottaker = opprettMottaker(forventetBrevMottakerFnr),
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
        coEvery { dokarkivService.journalfoer(any()) } returns journalpostResponse

        val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)
        runBlocking { service.journalfoerVedtaksbrev(vedtak, systembruker()) }

        verify(exactly = 1) {
            db.hentBrevInnhold(forventetBrev.id)
            db.hentPdf(forventetBrev.id)
            db.settBrevJournalfoert(forventetBrev.id, journalpostResponse)
        }

        val requestSlot = slot<JournalpostRequest>()
        coVerify(exactly = 1) {
            vedtaksbrevService.hentVedtaksbrev(forventetBrev.behandlingId!!)
            behandlingService.hentSak(forventetBrev.sakId, any())
            dokarkivService.journalfoer(capture(requestSlot))
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
                sakId = 41,
                behandlingId = null,
                tittel = null,
                spraak = Spraak.NB,
                prosessType = BrevProsessType.AUTOMATISK,
                soekerFnr = forventetBrevMottakerFnr,
                status = Status.FERDIGSTILT,
                statusEndret = Tidspunkt.now(),
                opprettet = Tidspunkt.now(),
                mottaker = opprettMottaker(forventetBrevMottakerFnr),
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
        coEvery { dokarkivService.journalfoer(any()) } returns journalpostResponse

        val sak = Sak(forventetBrev.soekerFnr, type, forventetBrev.sakId, Enheter.PORSGRUNN.enhetNr)
        coEvery { behandlingService.hentSak(any(), any()) } returns sak

        val service = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)
        runBlocking { service.journalfoer(forventetBrev.id, bruker) }

        verify(exactly = 1) {
            db.hentBrevInnhold(forventetBrev.id)
            db.hentPdf(forventetBrev.id)
            db.hentBrev(forventetBrev.id)
            db.settBrevJournalfoert(forventetBrev.id, journalpostResponse)
        }

        val requestSlot = slot<JournalpostRequest>()
        coVerify {
            behandlingService.hentSak(forventetBrev.sakId, bruker)
            dokarkivService.journalfoer(capture(requestSlot))
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
        prosessType: BrevProsessType,
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = Random.nextLong(10000),
        behandlingId = null,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        mottaker = opprettMottaker(SOEKER_FOEDSELSNUMMER.value),
        brevtype = Brevtype.INFORMASJON,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker(fnr: String) =
        Mottaker(
            "Stor Snerk",
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
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            behandlingId = UUID.randomUUID(),
            ansvarligEnhet = "1234",
            saksbehandler = "EY",
        )

    private companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}
