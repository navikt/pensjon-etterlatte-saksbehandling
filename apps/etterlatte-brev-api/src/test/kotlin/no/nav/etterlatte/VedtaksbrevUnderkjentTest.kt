package no.nav.etterlatte

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
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
import no.nav.etterlatte.rivers.VedtaksbrevUnderkjent
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

internal class VedtaksbrevUnderkjentTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val testRapid = TestRapid().apply { VedtaksbrevUnderkjent(this, vedtaksbrevService) }

    @BeforeEach
    fun before() = clearMocks(vedtaksbrevService)

    @AfterEach
    fun after() = confirmVerified(vedtaksbrevService)

    @Test
    fun `Vedtaksbrev finnes ikke`() {
        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns null

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id) }
    }

    @Test
    fun `Vedtaksbrev slettet ok`() {
        val brev = opprettBrev()

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        every { vedtaksbrevService.slettVedtaksbrev(any()) } returns true

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id) }
        verify(exactly = 1) { vedtaksbrevService.slettVedtaksbrev(brev.id) }
    }

    @Test
    fun `Feil ved sletting`() {
        val brev = opprettBrev()

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        every { vedtaksbrevService.slettVedtaksbrev(any()) } returns false

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        shouldThrow<Exception> {
            testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør
        }

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandling.id) }
        verify(exactly = 1) { vedtaksbrevService.slettVedtaksbrev(brev.id) }
    }

    private fun opprettMelding(vedtak: VedtakDto): JsonMessage {
        return JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to KafkaHendelseType.UNDERKJENT.toString(),
                "vedtak" to vedtak
            )
        )
    }

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto {
        return VedtakDto(
            vedtakId = 1L,
            status = VedtakStatus.RETURNERT,
            virkningstidspunkt = YearMonth.now(),
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            behandling = Behandling(behandlingType, UUID.randomUUID()),
            type = VedtakType.INNVILGELSE,
            utbetalingsperioder = emptyList(),
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now())
        )
    }

    private fun opprettBrev() = Brev(
        1,
        UUID.randomUUID(),
        "fnr",
        "tittel",
        Status.JOURNALFOERT,
        mottaker = mockk(),
        true
    )
}