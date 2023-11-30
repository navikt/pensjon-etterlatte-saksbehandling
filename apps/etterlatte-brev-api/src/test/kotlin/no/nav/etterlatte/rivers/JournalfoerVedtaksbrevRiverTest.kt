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
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakNyDto
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

internal class JournalfoerVedtaksbrevRiverTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val testRapid = TestRapid().apply { JournalfoerVedtaksbrevRiver(this, vedtaksbrevService) }

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
                "tittel",
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.FERDIGSTILT,
                Tidspunkt.now(),
                Tidspunkt.now(),
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
            vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId)
            vedtaksbrevService.journalfoerVedtaksbrev(any(), capture(vedtakCapture))
        }

        val vedtakActual = vedtakCapture.captured

        assertEquals(vedtak.id, vedtakActual.vedtakId)
        assertEquals(vedtak.behandlingId, vedtakActual.behandlingId)
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
                "tittel",
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.JOURNALFOERT,
                Tidspunkt.now(),
                Tidspunkt.now(),
                mottaker = mockk(),
            )

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        assertEquals(0, inspektoer.size)

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
    }

    @Test
    fun `Attestering av sak med behandlingstype MANUELT_OPPHOER`() {
        val vedtak = opprettVedtak(BehandlingType.MANUELT_OPPHOER)

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevEventTypes.FERDIGSTILT.name,
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
                    EVENT_NAME_KEY to BrevEventTypes.FERDIGSTILT.name,
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

        verify { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
    }

    private fun opprettMelding(vedtak: VedtakNyDto): JsonMessage {
        return JsonMessage.newMessage(
            mapOf(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                EVENT_NAME_KEY to VedtakKafkaHendelseType.ATTESTERT.toString(),
                "vedtak" to vedtak,
            ),
        )
    }

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakNyDto {
        val behandlingId = UUID.randomUUID()
        return VedtakNyDto(
            id = 1L,
            behandlingId = behandlingId,
            status = VedtakStatus.ATTESTERT,
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            type = VedtakType.INNVILGELSE,
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now()),
            innhold =
                VedtakInnholdDto.VedtakBehandlingDto(
                    virkningstidspunkt = YearMonth.now(),
                    behandling = Behandling(behandlingType, behandlingId),
                    utbetalingsperioder = emptyList(),
                ),
        )
    }

    private companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}
