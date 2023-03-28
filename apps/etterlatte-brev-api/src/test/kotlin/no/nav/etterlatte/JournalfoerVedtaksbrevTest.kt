package no.nav.etterlatte

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
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevEventTypes
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrev
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        val adresse = Adresse("Fornavn", "Etternavn", "Testgaten 13", "1234", "OSLO")
        val brev = Brev(
            1,
            BEHANDLING_ID,
            "fnr",
            "tittel",
            Status.FERDIGSTILT,
            Mottaker(STOR_SNERK, null, adresse),
            true
        )
        val response = JournalpostResponse("1234", null, null, true, emptyList())

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        every { vedtaksbrevService.journalfoerVedtaksbrev(any(), any()) } returns Pair(brev, response)

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        val vedtakCapture = slot<VedtakTilJournalfoering>()
        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id) }
        verify(exactly = 1) { vedtaksbrevService.journalfoerVedtaksbrev(any(), capture(vedtakCapture)) }

        val vedtakActual = vedtakCapture.captured

        assertEquals(vedtak.vedtakId, vedtakActual.vedtakId)
        assertEquals(vedtak.behandling.id, vedtakActual.behandlingId)
        assertEquals(vedtak.attestasjon!!.attesterendeEnhet, vedtakActual.ansvarligEnhet)

        val actualMessage = inspektoer.message(0)
        assertEquals(BrevEventTypes.JOURNALFOERT.toString(), actualMessage.get(EVENT_NAME_KEY).asText())
        assertEquals(brev.id, actualMessage.get("brevId").asLong())
        assertEquals(response.journalpostId, actualMessage.get("journalpostId").asText())
        assertEquals(DistribusjonsType.VEDTAK.toString(), actualMessage.get("distribusjonType").asText())
        assertEquals(adresse.toJson(), actualMessage.get("mottakerAdresse").toJson())
    }

    @Test
    fun `Brev er allerede journalfoert`() {
        val adresse = Adresse("Fornavn", "Etternavn", "Testgaten 13", "1234", "OSLO")
        val brev = Brev(
            1,
            BEHANDLING_ID,
            "fnr",
            "tittel",
            Status.JOURNALFOERT,
            Mottaker(STOR_SNERK, null, adresse),
            true
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

        val melding = JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to KafkaHendelseType.ATTESTERT.toString(),
                "vedtak" to vedtak
            )
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
                    EVENT_NAME_KEY to KafkaHendelseType.ATTESTERT.toString(),
                    "vedtak" to vedtak,
                    SKAL_SENDE_BREV to false
                )
            )

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify { vedtaksbrevService wasNot Called }
    }

    private fun opprettMelding(vedtak: VedtakDto): JsonMessage {
        return JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to KafkaHendelseType.ATTESTERT.toString(),
                "vedtak" to vedtak
            )
        )
    }

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto {
        return VedtakDto(
            vedtakId = 1L,
            status = VedtakStatus.ATTESTERT,
            virkningstidspunkt = YearMonth.now(),
            sak = Sak("Z123456", SakType.BARNEPENSJON, 2L),
            behandling = Behandling(behandlingType, UUID.randomUUID()),
            type = VedtakType.INNVILGELSE,
            utbetalingsperioder = emptyList(),
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now())
        )
    }

    private companion object {
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}