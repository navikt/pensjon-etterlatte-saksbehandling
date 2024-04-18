package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.JournalfoeringsMappingRequest
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random

class JournalfoerBrevServiceTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val sakService = mockk<SakService>()
    private val dokarkivService = mockk<DokarkivService>()
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val bruker = BrukerTokenInfo.of(UUID.randomUUID().toString(), "Z123456", null, null, null)

    @Test
    fun `Journalfoering fungerer som forventet`() {
        val brev = opprettBrev(Status.FERDIGSTILT, BrevProsessType.MANUELL)
        val sak = Sak("ident", SakType.BARNEPENSJON, brev.sakId, "1234")
        val journalpostResponse = OpprettJournalpostResponse("444", journalpostferdigstilt = true)

        val service = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)

        coEvery { sakService.hentSak(any(), any()) } returns sak
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
            db.settBrevJournalfoert(brev.id, journalpostResponse)
        }
        coVerify {
            sakService.hentSak(sak.id, bruker)
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
            sakService.hentSak(
                any(),
                any(),
            )
        } returns
            mockk<Sak>().also {
                every { it.sakType } returns SakType.BARNEPENSJON
                every { it.enhet } returns Enheter.UTLAND.enhetNr
            }
        val service = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)

        runBlocking {
            assertThrows<FeilStatusForJournalfoering> {
                service.journalfoer(brev.id, bruker)
            }
        }

        verify {
            db.hentBrev(brev.id)
        }
    }

    @Test
    fun `Brev finnes ikke for behandling`() {
        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns null

        val vedtak = opprettVedtak()

        val service = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)
        assertThrows<NoSuchElementException> {
            runBlocking { service.journalfoerVedtaksbrev(vedtak) }
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
            )

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev

        val vedtak = opprettVedtak()

        val service = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)
        runBlocking { service.journalfoerVedtaksbrev(vedtak) }

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
        coVerify(exactly = 0) { dokarkivService.journalfoer(any()) }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Journalfoeringsrequest for vedtaksbrev mappes korrekt`(type: SakType) {
        val forventetBrevMottakerFnr = "01018012345"
        val forventetBrev =
            Brev(
                id = 123,
                sakId = 41,
                behandlingId = null,
                tittel = null,
                spraak = Spraak.NB,
                prosessType = BrevProsessType.AUTOMATISK,
                soekerFnr = "soeker_fnr",
                status = Status.FERDIGSTILT,
                statusEndret = Tidspunkt.now(),
                opprettet = Tidspunkt.now(),
                mottaker =
                    Mottaker(
                        "Stor Snerk",
                        Foedselsnummer(forventetBrevMottakerFnr),
                        null,
                        Adresse(
                            adresseType = "NORSKPOSTADRESSE",
                            "Testgaten 13",
                            "1234",
                            "OSLO",
                            land = "Norge",
                            landkode = "NOR",
                        ),
                    ),
                brevtype = Brevtype.INFORMASJON,
            )

        coEvery { vedtaksbrevService.hentVedtaksbrev(any()) } returns forventetBrev
        every { db.hentBrev(any()) } returns forventetBrev

        val vedtak =
            VedtakTilJournalfoering(
                1,
                VedtakSak("ident", type, forventetBrev.sakId),
                UUID.randomUUID(),
                "ansvarligEnhet",
                "EY",
            )

        val service = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)
        coEvery { dokarkivService.journalfoer(any()) } returns
            OpprettJournalpostResponse(
                journalpostId = Random.nextLong().toString(),
                journalpostferdigstilt = true,
            )

        runBlocking { service.journalfoerVedtaksbrev(vedtak) }

        val requestSlot = slot<JournalfoeringsMappingRequest>()
        coVerify { dokarkivService.journalfoer(capture(requestSlot)) }
        verify {
            db.hentBrev(forventetBrev.id)
        }

        with(requestSlot.captured) {
            brevId shouldBe forventetBrev.id
            brev shouldBe forventetBrev
            brukerident shouldBe vedtak.sak.ident
            eksternReferansePrefiks shouldBe vedtak.behandlingId
            sakId shouldBe forventetBrev.sakId
            sakType shouldBe type
            journalfoerendeEnhet shouldBe vedtak.ansvarligEnhet
        }
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Journalfoeringsrequest for informasjonsbrev mappes korrekt`(type: SakType) {
        val forventetBrevMottakerFnr = "01018012345"
        val forventetBrev =
            Brev(
                id = 123,
                sakId = 41,
                behandlingId = null,
                tittel = null,
                spraak = Spraak.NB,
                prosessType = BrevProsessType.AUTOMATISK,
                soekerFnr = "soeker_fnr1",
                status = Status.FERDIGSTILT,
                statusEndret = Tidspunkt.now(),
                opprettet = Tidspunkt.now(),
                mottaker =
                    Mottaker(
                        "Stor Snerk",
                        Foedselsnummer(forventetBrevMottakerFnr),
                        null,
                        Adresse(
                            adresseType = "NORSKPOSTADRESSE",
                            "Testgaten 13",
                            "1234",
                            "OSLO",
                            land = "Norge",
                            landkode = "NOR",
                        ),
                    ),
                brevtype = Brevtype.MANUELT,
            )

        every { db.hentBrev(any()) } returns forventetBrev

        coEvery { dokarkivService.journalfoer(any()) } returns
            OpprettJournalpostResponse(
                "444",
                journalpostferdigstilt = true,
            )

        coEvery { sakService.hentSak(any(), any()) } returns
            Sak(
                ident = "I1",
                sakType = type,
                id = forventetBrev.sakId,
                enhet = "enhet1",
            )

        val service = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)
        runBlocking { service.journalfoer(forventetBrev.id, bruker) }

        val requestSlot = slot<JournalfoeringsMappingRequest>()
        coVerify { dokarkivService.journalfoer(capture(requestSlot)) }
        verify { db.hentBrev(forventetBrev.id) }

        with(requestSlot.captured) {
            brevId shouldBe forventetBrev.id
            brev shouldBe forventetBrev
            brukerident shouldBe forventetBrev.soekerFnr
            eksternReferansePrefiks shouldBe 41L
            sakId shouldBe forventetBrev.sakId
            sakType shouldBe type
            journalfoerendeEnhet shouldBe "enhet1"
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
        mottaker = opprettMottaker(),
        brevtype = Brevtype.INFORMASJON,
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
