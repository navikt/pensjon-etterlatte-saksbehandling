package no.nav.etterlatte.rivers

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
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
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class VedtaksbrevUnderkjentRiverTest {
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val testRapid = TestRapid().apply { VedtaksbrevUnderkjentRiver(this, vedtaksbrevService) }

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

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
    }

    @Test
    fun `Vedtaksbrev gjenåpnet ok`() {
        val brev = opprettBrev()

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        every { vedtaksbrevService.fjernFerdigstiltStatusUnderkjentVedtak(any(), any()) } returns true

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
        verify(exactly = 1) { vedtaksbrevService.fjernFerdigstiltStatusUnderkjentVedtak(brev.id, any()) }
    }

    @Test
    fun `Feil ved gjenåpning av vedtaksbrev`() {
        val brev = opprettBrev()

        every { vedtaksbrevService.hentVedtaksbrev(any()) } returns brev
        every { vedtaksbrevService.fjernFerdigstiltStatusUnderkjentVedtak(any(), any()) } returns false

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        shouldThrow<Exception> {
            testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør
        }

        verify(exactly = 1) { vedtaksbrevService.hentVedtaksbrev(vedtak.behandlingId) }
        verify(exactly = 1) { vedtaksbrevService.fjernFerdigstiltStatusUnderkjentVedtak(brev.id, any()) }
    }

    private fun opprettMelding(vedtak: VedtakDto): JsonMessage =
        JsonMessage.newMessage(
            mapOf(
                CORRELATION_ID_KEY to UUID.randomUUID().toString(),
                VedtakKafkaHendelseHendelseType.UNDERKJENT.lagParMedEventNameKey(),
                "vedtak" to vedtak,
            ),
        )

    private fun opprettVedtak(behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING): VedtakDto {
        val behandlingsid = UUID.randomUUID()
        return VedtakDto(
            id = 1L,
            behandlingId = behandlingsid,
            status = VedtakStatus.RETURNERT,
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, 2L),
            type = VedtakType.INNVILGELSE,
            vedtakFattet = VedtakFattet("Z00000", "1234", Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", "1234", Tidspunkt.now()),
            innhold =
                VedtakInnholdDto.VedtakBehandlingDto(
                    virkningstidspunkt = YearMonth.now(),
                    behandling = Behandling(behandlingType, behandlingsid),
                    utbetalingsperioder = emptyList(),
                    opphoerFraOgMed = null,
                ),
        )
    }

    private fun opprettBrev() =
        Brev(
            1,
            41,
            UUID.randomUUID(),
            "tittel",
            Spraak.NB,
            BrevProsessType.AUTOMATISK,
            "fnr",
            Status.JOURNALFOERT,
            Tidspunkt.now(),
            Tidspunkt.now(),
            mottaker = mockk(),
            brevtype = Brevtype.VEDTAK,
        )
}
