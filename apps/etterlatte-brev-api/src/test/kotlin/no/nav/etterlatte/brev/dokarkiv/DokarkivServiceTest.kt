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
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.journalpost.AvsenderMottaker
import no.nav.etterlatte.brev.journalpost.Bruker
import no.nav.etterlatte.brev.journalpost.JournalPostType
import no.nav.etterlatte.brev.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.journalpost.JournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.journalpost.Sak
import no.nav.etterlatte.brev.journalpost.Sakstype
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*
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
        val forventetResponse = JournalpostResponse("12345", journalpostferdigstilt = true)

        coEvery { mockKlient.opprettJournalpost(any(), any()) } returns forventetResponse
        every { mockDb.hentBrevInnhold(any()) } returns forventetInnhold
        every { mockDb.hentPdf(any()) } returns forventetPdf

        val brevId = Random.nextLong()

        val vedtak = VedtakTilJournalfoering(
            1,
            VedtakSak("ident", type, Random.nextLong()),
            UUID.randomUUID(),
            "ansvarligEnhet"
        )

        val response = service.journalfoer(brevId, vedtak)
        response shouldBe forventetResponse

        val requestSlot = slot<JournalpostRequest>()
        coVerify { mockKlient.opprettJournalpost(capture(requestSlot), true) }
        verify {
            mockDb.hentBrevInnhold(brevId)
            mockDb.hentPdf(brevId)
        }

        with(requestSlot.captured) {
            tittel shouldBe forventetInnhold.tittel
            journalpostType shouldBe JournalPostType.UTGAAENDE
            tema shouldBe vedtak.sak.sakType.tema
            kanal shouldBe "S"
            journalfoerendeEnhet shouldBe vedtak.ansvarligEnhet
            avsenderMottaker shouldBe AvsenderMottaker(vedtak.sak.ident)
            bruker shouldBe Bruker(vedtak.sak.ident)
            sak shouldBe Sak(Sakstype.FAGSAK, vedtak.sak.id.toString())
            eksternReferanseId shouldBe "${vedtak.behandlingId}.$brevId"

            with(dokumenter.single()) {
                tittel shouldBe forventetInnhold.tittel
                brevkode shouldBe BREV_KODE

                val dokument = dokumentvarianter.single()
                dokument.fysiskDokument shouldBe Base64.getEncoder().encodeToString(forventetPdf.bytes)
            }
        }
    }
}