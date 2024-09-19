package no.nav.etterlatte.rivers

import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class JournalfoerVedtaksbrevRiverTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()
    private val journalfoerBrevService = mockk<JournalfoerBrevService>()

    private val testRapid = TestRapid().apply { JournalfoerVedtaksbrevRiver(this, journalfoerBrevService) }

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
                Spraak.NB,
                BrevProsessType.AUTOMATISK,
                "fnr",
                Status.FERDIGSTILT,
                Tidspunkt.now(),
                Tidspunkt.now(),
                mottaker = mockk(),
                brevtype = Brevtype.VEDTAK,
                brevkoder = Brevkoder.BP_INNVILGELSE,
            )
        val response = OpprettJournalpostResponse("1234", true, emptyList())

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        coEvery { journalfoerBrevService.journalfoerVedtaksbrev(any(), any()) } returns Pair(response, 1)

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        val vedtakCapture = slot<VedtakTilJournalfoering>()
        coVerify(exactly = 1) {
            journalfoerBrevService.journalfoerVedtaksbrev(capture(vedtakCapture), any())
        }

        val vedtakActual = vedtakCapture.captured

        assertEquals(vedtak.id, vedtakActual.vedtakId)
        assertEquals(vedtak.behandlingId, vedtakActual.behandlingId)
        assertEquals(vedtak.attestasjon!!.attesterendeEnhet, vedtakActual.ansvarligEnhet)

        val actualMessage = inspektoer.message(0)
        assertEquals(BrevHendelseType.JOURNALFOERT.lagEventnameForType(), actualMessage.get(EVENT_NAME_KEY).asText())
        assertEquals(brev.id, actualMessage.get(BREV_ID_KEY).asLong())
        assertEquals(response.journalpostId, actualMessage.get("journalpostId").asText())
        assertEquals(DistribusjonsType.VEDTAK.toString(), actualMessage.get("distribusjonType").asText())
    }

    @Test
    fun `Attestering av sak med brevutsending false skal ikke sende ut brev`() {
        val vedtak = opprettVedtak(BehandlingType.REVURDERING)

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    VedtakKafkaHendelseHendelseType.ATTESTERT.lagParMedEventNameKey(),
                    "vedtak" to vedtak,
                    SKAL_SENDE_BREV to false,
                ),
            )

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify { vedtaksbrevService wasNot Called }
    }

    private fun opprettMelding(vedtak: VedtakDto): JsonMessage =
        JsonMessage.newMessage(
            mapOf(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                VedtakKafkaHendelseHendelseType.ATTESTERT.lagParMedEventNameKey(),
                "vedtak" to vedtak,
            ),
        )

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto {
        val behandlingId = UUID.randomUUID()
        return VedtakDto(
            id = 1L,
            behandlingId = behandlingId,
            status = VedtakStatus.ATTESTERT,
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            type = VedtakType.INNVILGELSE,
            vedtakFattet = VedtakFattet("Z00000", Enheter.defaultEnhet.enhetNr, Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", Enheter.defaultEnhet.enhetNr, Tidspunkt.now()),
            innhold =
                VedtakInnholdDto.VedtakBehandlingDto(
                    virkningstidspunkt = YearMonth.now(),
                    behandling = Behandling(behandlingType, behandlingId),
                    utbetalingsperioder = emptyList(),
                    opphoerFraOgMed = null,
                ),
        )
    }

    private companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}
