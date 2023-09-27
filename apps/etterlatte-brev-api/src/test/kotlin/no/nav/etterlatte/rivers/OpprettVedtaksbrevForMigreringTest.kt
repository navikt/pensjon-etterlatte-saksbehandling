package no.nav.etterlatte.rivers

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevEventTypes
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class OpprettVedtaksbrevForMigreringTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val testRapid = TestRapid().apply { OpprettVedtaksbrevForMigrering(this, vedtaksbrevService) }

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun before() = clearMocks(vedtaksbrevService)

    @AfterEach
    fun after() = confirmVerified(vedtaksbrevService)

    @Test
    fun `oppretter for migrering nytt brev, genererer pdf og sender videre`() {
        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak, Vedtaksloesning.PESYS)
        val brev = opprettBrev()

        coEvery { vedtaksbrevService.opprettVedtaksbrev(any(), behandlingId, any()) } returns brev
        coEvery { vedtaksbrevService.genererPdf(brev.id, any()) } returns mockk<Pdf>()

        val inspektoer = testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        coVerify(exactly = 1) { vedtaksbrevService.opprettVedtaksbrev(any(), behandlingId, any()) }
        coVerify(exactly = 1) { vedtaksbrevService.genererPdf(brev.id, any()) }

        val meldingSendt = inspektoer.message(0)
        assertEquals(BrevEventTypes.FERDIGSTILT.name, meldingSendt.get(EVENT_NAME_KEY).asText())
    }

    @Test
    fun `sender for sak med opphav i Gjenny kun videre`() {
        val inspektoer =
            testRapid.apply {
                sendTestMessage(
                    opprettMelding(
                        opprettVedtak(),
                        Vedtaksloesning.GJENNY,
                    ).toJson(),
                )
            }.inspektør

        assertEquals(BrevEventTypes.FERDIGSTILT.name, inspektoer.message(0).get(EVENT_NAME_KEY).asText())
    }

    private fun opprettBrev() =
        Brev(
            1,
            41,
            behandlingId,
            BrevProsessType.AUTOMATISK,
            "fnr",
            Status.FERDIGSTILT,
            mottaker = mockk(),
        )

    private fun opprettMelding(
        vedtak: VedtakDto,
        kilde: Vedtaksloesning,
    ): JsonMessage =
        JsonMessage.newMessage(
            mapOf(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                EVENT_NAME_KEY to KafkaHendelseType.ATTESTERT.toString(),
                "vedtak" to vedtak,
                KILDE_KEY to kilde,
            ),
        )

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto =
        VedtakDto(
            vedtakId = 1L,
            status = VedtakStatus.ATTESTERT,
            virkningstidspunkt = YearMonth.now(),
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            behandling = Behandling(behandlingType, behandlingId),
            type = VedtakType.INNVILGELSE,
            utbetalingsperioder = emptyList(),
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now()),
        )
}
