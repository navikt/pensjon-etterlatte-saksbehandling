
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.MigreringHendelser
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.YearMonth
import java.util.UUID

class MigreringHendelserTest {
    private val vedtakService: VedtakService = mockk()
    private val inspector = TestRapid().apply { MigreringHendelser(this, vedtakService) }

    @Test
    fun `hvis opprett vedtak feila, legg meldinga rett paa feila-koea`() {
        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.VEDTAK,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    SAK_ID_KEY to "1",
                ),
            )
        coEvery {
            vedtakService.opprettVedtakFattOgAttester(any(), any())
        } throws RuntimeException("Feila under opprett vedtak")

        inspector.sendTestMessage(melding.toJson())

        Assertions.assertEquals(1, inspector.inspektør.size)
        val sendtMelding = inspector.inspektør.message(0)
        Assertions.assertEquals(sendtMelding.get(EVENT_NAME_KEY).asText(), EventNames.FEILA)
    }

    @Test
    fun `oppretter vedtak, fatter vedtak og attesterer`() {
        val behandlingId = "a9d42eb9-561f-4320-8bba-2ba600e66e21"
        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.VEDTAK,
                mapOf(
                    BEHANDLING_ID_KEY to behandlingId,
                    SAK_ID_KEY to "1",
                ),
            )
        val vedtakDto =
            VedtakDto(
                123,
                VedtakStatus.OPPRETTET,
                YearMonth.now(),
                VedtakSak("1", SakType.BARNEPENSJON, 1),
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, UUID.fromString(behandlingId)),
                VedtakType.INNVILGELSE,
                null,
                null,
                listOf(),
            )
        coEvery { vedtakService.opprettVedtakFattOgAttester(any(), any()) } returns
            VedtakOgRapid(
                vedtakDto,
                RapidInfo(
                    vedtakhendelse = VedtakKafkaHendelseType.ATTESTERT,
                    vedtak = vedtakDto,
                    tekniskTid = Tidspunkt.now(),
                    behandlingId = UUID.fromString(behandlingId),
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        coVerify { vedtakService.opprettVedtakFattOgAttester(any(), any()) }

        Assertions.assertEquals(1, inspector.inspektør.size)
        val sendtTilRapid = inspector.inspektør.message(0)
        Assertions.assertEquals(VedtakKafkaHendelseType.ATTESTERT.toString(), sendtTilRapid.get(EVENT_NAME_KEY).textValue())
        Assertions.assertEquals(sendtTilRapid.get(BEHANDLING_ID_KEY).textValue(), behandlingId)
    }
}
