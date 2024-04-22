package no.nav.etterlatte.brev.dokarkiv

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.Base64
import java.util.UUID
import kotlin.random.Random

internal class DokarkivServiceTest {
    private val mockKlient = mockk<DokarkivKlient>()
    private val mockDb = mockk<BrevRepository>()

    private val service = DokarkivServiceImpl(mockKlient, mockDb)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified()
    }

    @ParameterizedTest
    @EnumSource(SakType::class)
    fun `Journalfoeringsrequest mappes korrekt`(type: SakType) {
        val forventetInnhold = BrevInnhold("tittel", Spraak.NB, mockk())
        val forventetPdf = Pdf("Hello world!".toByteArray())
        val forventetBrevMottakerFnr = SOEKER2_FOEDSELSNUMMER.value

        val brevId = Random.nextLong()
        val sakId = Random.nextLong()

        val forventetBrev =
            Brev(
                id = brevId,
                sakId = sakId,
                behandlingId = null,
                tittel = null,
                spraak = Spraak.NB,
                prosessType = BrevProsessType.AUTOMATISK,
                soekerFnr = "soeker_fnr",
                status = Status.OPPRETTET,
                statusEndret = Tidspunkt.now(),
                opprettet = Tidspunkt.now(),
                mottaker =
                    Mottaker(
                        "Stor Snerk",
                        Folkeregisteridentifikator.of(forventetBrevMottakerFnr),
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
        val forventetResponse = OpprettJournalpostResponse("12345", journalpostferdigstilt = true)

        coEvery { mockKlient.opprettJournalpost(any(), any()) } returns forventetResponse
        every { mockDb.hentBrevInnhold(any()) } returns forventetInnhold
        every { mockDb.hentPdf(any()) } returns forventetPdf

        val vedtak =
            VedtakTilJournalfoering(
                1,
                VedtakSak("ident", type, sakId),
                UUID.randomUUID(),
                "ansvarligEnhet",
                "A123",
            )

        val request =
            JournalfoeringsMappingRequest(
                brevId = brevId,
                brev = forventetBrev,
                brukerident = vedtak.sak.ident,
                eksternReferansePrefiks = vedtak.behandlingId,
                sakId = sakId,
                sakType = type,
                journalfoerendeEnhet = vedtak.ansvarligEnhet,
            )
        val response = runBlocking { service.journalfoer(request) }
        response shouldBe forventetResponse

        val requestSlot = slot<OpprettJournalpostRequest>()
        coVerify { mockKlient.opprettJournalpost(capture(requestSlot), true) }
        verify {
            mockDb.hentBrevInnhold(brevId)
            mockDb.hentPdf(brevId)
        }

        with(requestSlot.captured) {
            tittel shouldBe forventetInnhold.tittel
            journalposttype shouldBe JournalPostType.UTGAAENDE
            tema shouldBe vedtak.sak.sakType.tema
            kanal shouldBe "S"
            journalfoerendeEnhet shouldBe vedtak.ansvarligEnhet
            avsenderMottaker shouldBe AvsenderMottaker(forventetBrevMottakerFnr, navn = "Stor Snerk")
            bruker shouldBe Bruker(vedtak.sak.ident)
            sak shouldBe JournalpostSak(Sakstype.FAGSAK, vedtak.sak.id.toString(), vedtak.sak.sakType.tema, "EY")
            eksternReferanseId shouldBe "${vedtak.behandlingId}.$brevId"

            with(dokumenter.single()) {
                tittel shouldBe forventetInnhold.tittel
                brevkode shouldBe BREV_KODE

                val dokument = dokumentvarianter.single()
                dokument.fysiskDokument shouldBe Base64.getEncoder().encodeToString(forventetPdf.bytes)
            }
        }
    }

    @Nested
    inner class OppdaterJournalpost {
        @Test
        fun `Fagsaksystem FS22 mappes korrekt`() {
            val journalpostId = "1"

            coEvery { mockKlient.oppdaterJournalpost(any(), any()) } returns OppdaterJournalpostResponse(journalpostId)

            val request =
                OppdaterJournalpostRequest(
                    sak = JournalpostSak(fagsaksystem = "FS22", sakstype = Sakstype.GENERELL_SAK, tema = "EYO"),
                )

            runBlocking {
                service.oppdater(journalpostId, false, null, request)
            }

            val requestSlot = slot<OppdaterJournalpostRequest>()
            coVerify { mockKlient.oppdaterJournalpost(journalpostId, capture(requestSlot)) }

            with(requestSlot.captured) {
                sak?.fagsakId shouldBe null
                sak?.fagsaksystem shouldBe null
                sak?.sakstype shouldBe Sakstype.GENERELL_SAK
                sak?.tema shouldBe null
            }
        }

        @Test
        fun `Fagsaksystem EY mappes korrekt`() {
            val journalpostId = "1"

            coEvery { mockKlient.oppdaterJournalpost(any(), any()) } returns OppdaterJournalpostResponse(journalpostId)

            val request =
                OppdaterJournalpostRequest(
                    sak =
                        JournalpostSak(
                            fagsaksystem = Fagsaksystem.EY.navn,
                            sakstype = Sakstype.FAGSAK,
                            tema = "EYO",
                            fagsakId = "1234",
                        ),
                )

            runBlocking {
                service.oppdater(journalpostId, false, null, request)
            }

            val requestSlot = slot<OppdaterJournalpostRequest>()
            coVerify { mockKlient.oppdaterJournalpost(journalpostId, capture(requestSlot)) }

            with(requestSlot.captured) {
                sak?.fagsakId shouldBe "1234"
                sak?.fagsaksystem shouldBe "EY"
                sak?.sakstype shouldBe Sakstype.FAGSAK
                sak?.tema shouldBe "EYO"
            }
        }
    }
}
