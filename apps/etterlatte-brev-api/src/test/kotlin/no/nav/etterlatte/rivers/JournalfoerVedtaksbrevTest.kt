package no.nav.etterlatte.rivers

import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID

internal class JournalfoerVedtaksbrevTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val testRapid = TestRapid().apply { JournalfoerVedtaksbrev(this, vedtaksbrevService) }

    @BeforeEach
    fun before() = clearMocks(vedtaksbrevService)

    @AfterEach
    fun after() = confirmVerified(vedtaksbrevService)

    @Test
    fun `Gyldig melding skal sende journalpost til distribusjon`() {
        val brev =
            Brev(
                1,
                41,
                BEHANDLING_ID,
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.FERDIGSTILT,
                mottaker = mockk(),
            )
        val response = JournalpostResponse("1234", null, null, true, emptyList())

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        every { vedtaksbrevService.journalfoerVedtaksbrev(any(), any()) } returns response

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        val vedtakCapture = slot<VedtakTilJournalfoering>()
        verify(exactly = 1) {
            vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id)
            vedtaksbrevService.journalfoerVedtaksbrev(any(), capture(vedtakCapture))
        }

        val vedtakActual = vedtakCapture.captured

        assertEquals(vedtak.vedtakId, vedtakActual.vedtakId)
        assertEquals(vedtak.behandling.id, vedtakActual.behandlingId)
        assertEquals(vedtak.attestasjon!!.attesterendeEnhet, vedtakActual.ansvarligEnhet)

        val actualMessage = inspektoer.message(0)
        assertEquals(BrevEventTypes.JOURNALFOERT.toString(), actualMessage.get(EVENT_NAME_KEY).asText())
        assertEquals(brev.id, actualMessage.get("brevId").asLong())
        assertEquals(response.journalpostId, actualMessage.get("journalpostId").asText())
        assertEquals(DistribusjonsType.VEDTAK.toString(), actualMessage.get("distribusjonType").asText())
    }

    @Test
    fun `Brev er allerede journalfoert`() {
        val brev =
            Brev(
                1,
                41,
                BEHANDLING_ID,
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.JOURNALFOERT,
                mottaker = mockk(),
            )

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        assertEquals(0, inspektoer.size)

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id) }
    }

    @Test
    fun `Attestering av sak med behandlingstype MANUELT_OPPHOER`() {
        val vedtak = opprettVedtak(BehandlingType.MANUELT_OPPHOER)

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevEventTypes.FERDIGSTILT.toEventName(),
                    "vedtak" to vedtak,
                ),
            )

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify { vedtaksbrevService wasNot Called }
    }

    @Test
    fun `Attestering av sak med brevutsending false skal ikke sende ut brev`() {
        val vedtak = opprettVedtak(BehandlingType.REVURDERING)

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevEventTypes.FERDIGSTILT.toEventName(),
                    "vedtak" to vedtak,
                    SKAL_SENDE_BREV to false,
                ),
            )

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify { vedtaksbrevService wasNot Called }
    }

    @Test
    fun `Brev finnes ikke for behandling`() {
        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns null

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        assertThrows<NoSuchElementException> {
            testRapid.apply { sendTestMessage(melding.toJson()) }
        }

        verify { vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id) }
    }

    private fun opprettMelding(vedtak: VedtakDto): JsonMessage {
        return JsonMessage.newMessage(
            mapOf(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                EVENT_NAME_KEY to BrevEventTypes.FERDIGSTILT.toEventName(),
                "vedtak" to vedtak,
            ),
        )
    }

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto {
        return VedtakDto(
            vedtakId = 1L,
            status = VedtakStatus.ATTESTERT,
            virkningstidspunkt = YearMonth.now(),
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            behandling = Behandling(behandlingType, UUID.randomUUID()),
            type = VedtakType.INNVILGELSE,
            utbetalingsperioder = emptyList(),
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now()),
        )
    }

    private companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}
