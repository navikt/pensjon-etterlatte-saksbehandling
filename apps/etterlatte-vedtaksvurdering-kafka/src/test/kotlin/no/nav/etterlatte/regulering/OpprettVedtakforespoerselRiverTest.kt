package no.nav.etterlatte.regulering

import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.no.nav.etterlatte.regulering.ReguleringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OpprettVedtakforespoerselRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)
    private val sakId = randomSakId()

    private fun genererOpprettVedtakforespoersel(behandlingId: UUID) =
        JsonMessage.newMessage(
            mapOf(
                OmregningHendelseType.BEREGNA.lagParMedEventNameKey(),
                HENDELSE_DATA_KEY to
                    OmregningData(
                        "kjoering",
                        sakId = sakId,
                        fradato = foersteMai2023,
                        revurderingaarsak = Revurderingaarsak.REGULERING,
                        behandlingId = behandlingId,
                    ).toPacket(),
            ),
        )

    @Test
    fun `skal baade opprette vedtak og fatte det samt attestere`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    DummyFeatureToggleService(),
                )
            }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakFattOgAttester(sakId, behandlingId) }
    }

    @Test
    fun `skal opprette vedtak og bare fatte hvis feature toggle for stopp er paa`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val featureToggleService = DummyFeatureToggleService()
        featureToggleService.settBryter(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, true)

        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    featureToggleService,
                )
            }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
    }

    @Test
    fun `skal baade opprette vedtak og fatte det samt attestere hvis feature toggle for stopp er av`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val featureToggleService = DummyFeatureToggleService()
        featureToggleService.settBryter(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, false)

        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    featureToggleService,
                )
            }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakFattOgAttester(sakId, behandlingId) }
    }
}
