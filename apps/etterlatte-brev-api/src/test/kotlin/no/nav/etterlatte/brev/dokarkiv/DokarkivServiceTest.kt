package no.nav.etterlatte.brev.dokarkiv

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DokarkivServiceTest {
    private val mockKlient = mockk<DokarkivKlient>()

    private val service = DokarkivServiceImpl(mockKlient)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(mockKlient)
    }

    @Test
    fun `Opprett journalpost`() {
        val forventetResponse = OpprettJournalpostResponse("12345", journalpostferdigstilt = true)

        coEvery { mockKlient.opprettJournalpost(any<OpprettJournalpost>(), any(), any()) } returns forventetResponse

        val request = mockk<JournalpostRequest>(relaxed = true)
        val response = runBlocking { service.journalfoer(request, simpleSaksbehandler()) }

        response.journalpostId shouldBe forventetResponse.journalpostId

        coVerify { mockKlient.opprettJournalpost(request, true, any()) }
    }

    @Nested
    inner class OppdaterJournalpost {
        @Test
        fun `Fagsaksystem FS22 mappes korrekt`() {
            val journalpostId = "1"

            coEvery { mockKlient.oppdaterJournalpost(any(), any(), any()) } returns OppdaterJournalpostResponse(journalpostId)

            val request =
                OppdaterJournalpostRequest(
                    sak = JournalpostSak(fagsaksystem = "FS22", sakstype = Sakstype.GENERELL_SAK, tema = "EYO"),
                )

            runBlocking {
                service.oppdater(journalpostId, false, null, request, simpleSaksbehandler())
            }

            val requestSlot = slot<OppdaterJournalpostRequest>()
            coVerify { mockKlient.oppdaterJournalpost(journalpostId, capture(requestSlot), any()) }

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

            coEvery { mockKlient.oppdaterJournalpost(any(), any(), any()) } returns OppdaterJournalpostResponse(journalpostId)

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
                service.oppdater(journalpostId, false, null, request, simpleSaksbehandler())
            }

            val requestSlot = slot<OppdaterJournalpostRequest>()
            coVerify { mockKlient.oppdaterJournalpost(journalpostId, capture(requestSlot), any()) }

            with(requestSlot.captured) {
                sak?.fagsakId shouldBe "1234"
                sak?.fagsaksystem shouldBe "EY"
                sak?.sakstype shouldBe Sakstype.FAGSAK
                sak?.tema shouldBe "EYO"
            }
        }
    }
}
