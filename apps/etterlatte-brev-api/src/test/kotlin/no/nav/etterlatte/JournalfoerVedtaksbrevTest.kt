package no.nav.etterlatte

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
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
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
        val brev = Brev(1, "2", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK, null, adresse), true)
        val response = JournalpostResponse("1234", null, null, true, emptyList())

        every { vedtaksbrevService.journalfoerVedtaksbrev(any()) } returns Pair(brev, response)

        val vedtak = opprettVedtak()

        val melding = JsonMessage.newMessage(
            mapOf(
                eventNameKey to KafkaHendelseType.ATTESTERT.toString(),
                "vedtak" to vedtak
            )
        )

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        val vedtakCapture = slot<Vedtak>()
        verify(exactly = 1) { vedtaksbrevService.journalfoerVedtaksbrev(capture(vedtakCapture)) }

        val vedtakActual = vedtakCapture.captured

        assertEquals(vedtak.vedtakId, vedtakActual.vedtakId)
        assertEquals(vedtak.virk, vedtakActual.virk)
        assertEquals(vedtak.sak, vedtakActual.sak)
        assertEquals(vedtak.behandling, vedtakActual.behandling)
        assertEquals(vedtak.type, vedtakActual.type)
        assertEquals(vedtak.grunnlag, vedtakActual.grunnlag)
        assertEquals(vedtak.pensjonTilUtbetaling, vedtakActual.pensjonTilUtbetaling)
        assertEquals(vedtak.vedtakFattet!!.ansvarligSaksbehandler, vedtakActual.vedtakFattet!!.ansvarligSaksbehandler)
        assertEquals(vedtak.vedtakFattet!!.ansvarligEnhet, vedtakActual.vedtakFattet!!.ansvarligEnhet)
        assertEquals(vedtak.attestasjon!!.attestant, vedtakActual.attestasjon!!.attestant)
        assertEquals(vedtak.attestasjon!!.attesterendeEnhet, vedtakActual.attestasjon!!.attesterendeEnhet)

        val actualMessage = inspektoer.message(0)
        assertEquals(BrevEventTypes.JOURNALFOERT.toString(), actualMessage.get(eventNameKey).asText())
        assertEquals(brev.id, actualMessage.get("brevId").asLong())
        assertEquals(response.journalpostId, actualMessage.get("journalpostId").asText())
        assertEquals(DistribusjonsType.VEDTAK.toString(), actualMessage.get("distribusjonType").asText())
        assertEquals(adresse.toJson(), actualMessage.get("mottakerAdresse").toJson())
    }

    private fun opprettVedtak(): Vedtak {
        return Vedtak(
            vedtakId = 1L,
            virk = Periode(YearMonth.now(), YearMonth.now().plusYears(2)),
            sak = Sak("Z123456", SakType.BARNEPENSJON, 2L),
            behandling = Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, UUID.randomUUID()),
            type = VedtakType.INNVILGELSE,
            grunnlag = emptyList(),
            vilkaarsvurdering = null,
            beregning = null,
            pensjonTilUtbetaling = emptyList(),
            vedtakFattet = VedtakFattet("Z00000", "1234", ZonedDateTime.now(ZoneId.of("Europe/Oslo"))),
            attestasjon = Attestasjon("Z00000", "1234", ZonedDateTime.now(ZoneId.of("Europe/Oslo")))
        )
    }

    private companion object {
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
    }
}