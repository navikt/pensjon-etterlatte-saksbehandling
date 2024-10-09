package no.nav.etterlatte.rivers

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
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
    private val brevApiKlient = mockk<BrevapiKlient>()

    private val testRapid = TestRapid().apply { VedtaksbrevUnderkjentRiver(this, brevApiKlient) }

    @BeforeEach
    fun before() = clearMocks(brevApiKlient)

    @AfterEach
    fun after() = confirmVerified(brevApiKlient)

    @Test
    fun `Vedtaksbrev finnes ikke`() {
        coEvery { brevApiKlient.hentVedtaksbrev(any()) } returns null

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        coVerify(exactly = 1) { brevApiKlient.hentVedtaksbrev(vedtak.behandlingId) }
    }

    @Test
    fun `Vedtaksbrev gjenåpnet ok`() {
        val brev = opprettBrev()

        coEvery { brevApiKlient.hentVedtaksbrev(any()) } returns brev
        coEvery { brevApiKlient.fjernFerdigstiltStatusUnderkjentVedtak(any(), any()) } just runs

        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør

        coVerify(exactly = 1) { brevApiKlient.hentVedtaksbrev(vedtak.behandlingId) }
        coVerify(exactly = 1) {
            brevApiKlient.fjernFerdigstiltStatusUnderkjentVedtak(
                match {
                    it.vedtak == vedtak.toJsonNode()
                    it.vedtaksbrev == brev
                },
                any(),
            )
        }
    }

    @Test
    fun `Feil ved gjenåpning av vedtaksbrev`() {
        val brev = opprettBrev()

        coEvery { brevApiKlient.hentVedtaksbrev(any()) } returns brev
        coEvery { brevApiKlient.fjernFerdigstiltStatusUnderkjentVedtak(any(), any()) } throws
            ForespoerselException(500, "UKJENT_FEIL_HENT_VEDTAKSBREV", "Kunne ikke hente vedtaksbrev for behandlingid: xyz")
        val vedtak = opprettVedtak()
        val melding = opprettMelding(vedtak)

        shouldThrow<Exception> {
            testRapid.apply { sendTestMessage(melding.toJson()) }.inspektør
        }

        coVerify(exactly = 1) { brevApiKlient.hentVedtaksbrev(vedtak.behandlingId) }
        coVerify(exactly = 1) {
            brevApiKlient.fjernFerdigstiltStatusUnderkjentVedtak(
                match {
                    it.vedtak == vedtak.toJsonNode()
                    it.vedtaksbrev == brev
                },
                any(),
            )
        }
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
            sak = VedtakSak("Z123456", SakType.BARNEPENSJON, sakId2),
            type = VedtakType.INNVILGELSE,
            vedtakFattet = VedtakFattet("Z00000", Enheter.defaultEnhet.enhetNr, Tidspunkt.now()),
            attestasjon = Attestasjon("Z00000", Enheter.defaultEnhet.enhetNr, Tidspunkt.now()),
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
            randomSakId(),
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
            brevkoder = Brevkoder.BP_AVSLAG,
        )
}
