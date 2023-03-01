package no.nav.etterlatte.brev.dokarkiv

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevInnhold
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.DokumentVariant
import no.nav.etterlatte.libs.common.journalpost.JournalPostType
import no.nav.etterlatte.libs.common.journalpost.JournalpostKoder.Companion.BEHANDLINGSTEMA_BP
import no.nav.etterlatte.libs.common.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.journalpost.Sakstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*
import no.nav.etterlatte.libs.common.journalpost.Sak as JSak
import no.nav.etterlatte.libs.common.sak.Sak as VSak

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
        confirmVerified(mockKlient, mockDb)
    }

    @Test
    fun `Journalfoering`() {
        every { mockDb.hentBrevInnhold(any()) } returns BrevInnhold("mal", Spraak.NB, "".toByteArray())
        coEvery { mockKlient.opprettJournalpost(any(), any()) } returns JournalpostResponse("id", "OK", "melding", true)

        val vedtaksbrev = opprettVedtaksbrev()
        val vedtak = opprettVedtak()

        service.journalfoer(vedtaksbrev, vedtak)

        val requestSlot = slot<JournalpostRequest>()
        coVerify(exactly = 1) { mockKlient.opprettJournalpost(capture(requestSlot), true) }
        verify(exactly = 1) { mockDb.hentBrevInnhold(vedtaksbrev.id) }

        val actualRequest = requestSlot.captured

        assertEquals(vedtaksbrev.tittel, actualRequest.tittel)
        assertEquals(JournalPostType.UTGAAENDE, actualRequest.journalpostType)
        assertEquals(BEHANDLINGSTEMA_BP, actualRequest.behandlingstema)

        assertEquals(vedtaksbrev.mottaker.foedselsnummer!!.value, actualRequest.avsenderMottaker.id)

        assertEquals(Bruker(vedtak.sak.ident), actualRequest.bruker)
        assertEquals("${vedtaksbrev.behandlingId}.${vedtaksbrev.id}", actualRequest.eksternReferanseId)
        assertEquals(JSak(Sakstype.FAGSAK, vedtaksbrev.behandlingId.toString()), actualRequest.sak)

        val dokument = actualRequest.dokumenter.single()
        assertEquals(vedtaksbrev.tittel, dokument.tittel)
        assertEquals(BREV_KODE, dokument.brevkode)
        assertTrue(dokument.dokumentvarianter.single() is DokumentVariant.ArkivPDF)

        assertEquals("EYB", actualRequest.tema)
        assertEquals("S", actualRequest.kanal)
        assertEquals(vedtak.vedtakFattet!!.ansvarligEnhet, actualRequest.journalfoerendeEnhet)
    }

    private fun opprettVedtaksbrev() = Brev(
        id = 1,
        behandlingId = UUID.randomUUID(),
        soekerFnr = "soeker fnr",
        tittel = "vedtak om innvilgelse",
        status = Status.FERDIGSTILT,
        mottaker = Mottaker(STOR_SNERK),
        erVedtaksbrev = true
    )

    private fun opprettVedtak() = VedtakDto(
        vedtakId = 1,
        virkningstidspunkt = YearMonth.now(),
        behandling = Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, UUID.randomUUID()),
        type = VedtakType.INNVILGELSE,
        sak = VSak("ident", SakType.BARNEPENSJON, 1),
        vedtakFattet = VedtakFattet("Z12345", "4808", ZonedDateTime.now()),
        utbetalingsperioder = emptyList(),
        attestasjon = null
    )

    companion object {
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
    }
}