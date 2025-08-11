package no.nav.etterlatte.grunnlag

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsversjoneringRiverTest {
    private val grunnlagKlientMock = mockk<GrunnlagKlient>(relaxed = true)

    private lateinit var inspector: TestRapid

    @BeforeAll
    fun beforeAll() {
        inspector =
            TestRapid().apply {
                GrunnlagsversjoneringRiver(this, grunnlagKlientMock)
            }
    }

    @Test
    fun `Grunnlaget skal låses når vedtak attesteres`() {
        val behandlingId = UUID.randomUUID()

        val vedtak =
            VedtakDto(
                id = Random.nextLong(),
                behandlingId = behandlingId,
                status = VedtakStatus.ATTESTERT,
                sak =
                    VedtakSak(
                        "ident",
                        SakType.BARNEPENSJON,
                        SakId(
                            Random.nextLong(),
                        ),
                    ),
                type = VedtakType.INNVILGELSE,
                vedtakFattet = null,
                attestasjon = null,
                innhold =
                    VedtakInnholdDto.VedtakBehandlingDto(
                        virkningstidspunkt = YearMonth.now(),
                        behandling = Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, UUID.randomUUID(), null),
                        utbetalingsperioder = emptyList(),
                        opphoerFraOgMed = null,
                    ),
            )

        val melding =
            JsonMessage
                .newMessage(
                    mapOf(
                        VedtakKafkaHendelseHendelseType.ATTESTERT.lagParMedEventNameKey(),
                        "vedtak" to vedtak,
                    ),
                ).toJson()

        inspector.sendTestMessage(melding)

        verify { grunnlagKlientMock.laasVersjonForBehandling(behandlingId) }
    }
}
