package no.nav.etterlatte

import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.nowNorskTid
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrev
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

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

        every { vedtaksbrevService.journalfoerVedtaksbrev(any()) } returns Pair(brev, response)

        val vedtak = opprettVedtak()

        val melding = JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to KafkaHendelseType.ATTESTERT.toString(),
                "vedtak" to vedtak
            )
        )

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        val vedtakDtoCapture = slot<VedtakDto>()
        verify(exactly = 1) { vedtaksbrevService.journalfoerVedtaksbrev(capture(vedtakDtoCapture)) }

        val vedtakActual = vedtakDtoCapture.captured

        assertEquals(vedtak.vedtakId, vedtakActual.vedtakId)
        assertEquals(vedtak.virkningstidspunkt, vedtakActual.virkningstidspunkt)
        assertEquals(vedtak.sak, vedtakActual.sak)
        assertEquals(vedtak.behandling, vedtakActual.behandling)
        assertEquals(vedtak.type, vedtakActual.type)
        assertEquals(vedtak.utbetalingsperioder, vedtakActual.utbetalingsperioder)
        assertEquals(vedtak.vedtakFattet!!.ansvarligSaksbehandler, vedtakActual.vedtakFattet!!.ansvarligSaksbehandler)
        assertEquals(vedtak.vedtakFattet!!.ansvarligEnhet, vedtakActual.vedtakFattet!!.ansvarligEnhet)
        assertEquals(vedtak.attestasjon!!.attestant, vedtakActual.attestasjon!!.attestant)
        assertEquals(vedtak.attestasjon!!.attesterendeEnhet, vedtakActual.attestasjon!!.attesterendeEnhet)

        val actualMessage = inspektoer.message(0)
        assertEquals(BrevEventTypes.JOURNALFOERT.toString(), actualMessage.get(EVENT_NAME_KEY).asText())
        assertEquals(brev.id, actualMessage.get("brevId").asLong())
        assertEquals(response.journalpostId, actualMessage.get("journalpostId").asText())
        assertEquals(DistribusjonsType.VEDTAK.toString(), actualMessage.get("distribusjonType").asText())
        assertEquals(adresse.toJson(), actualMessage.get("mottakerAdresse").toJson())
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

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto {
        return VedtakDto(
            vedtakId = 1L,
            virkningstidspunkt = YearMonth.now(),
            sak = Sak("Z123456", SakType.BARNEPENSJON, 2L),
            behandling = Behandling(behandlingType, UUID.randomUUID()),
            type = VedtakType.INNVILGELSE,
            utbetalingsperioder = emptyList(),
            vedtakFattet = VedtakFattet("Z00000", "1234", nowNorskTid()),
            attestasjon = Attestasjon("Z00000", "1234", nowNorskTid())
        )
    }

    private companion object {
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}