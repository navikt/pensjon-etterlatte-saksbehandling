package no.nav.etterlatte.regulering

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.no.nav.etterlatte.klienter.UtbetalingKlient
import no.nav.etterlatte.no.nav.etterlatte.regulering.ReguleringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class OpprettVedtakforespoerselRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)
    private val sakId = randomSakId()

    private fun genererOpprettVedtakforespoersel(
        behandlingId: UUID,
        revurderingaarsak: Revurderingaarsak = Revurderingaarsak.REGULERING,
    ) = JsonMessage.newMessage(
        mapOf(
            OmregningHendelseType.BEREGNA.lagParMedEventNameKey(),
            HENDELSE_DATA_KEY to
                OmregningData(
                    "kjoering",
                    sakId = sakId,
                    fradato = foersteMai2023,
                    revurderingaarsak = revurderingaarsak,
                    behandlingId = behandlingId,
                ).toPacket(),
        ),
    )

    @Test
    fun `skal baade opprette vedtak og fatte det samt attestere`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val utbetalingKlientMock = mockk<UtbetalingKlient>(relaxed = true)
        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    utbetalingKlientMock,
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
        val utbetalingKlientMock = mockk<UtbetalingKlient>(relaxed = true)
        val featureToggleService = DummyFeatureToggleService()
        featureToggleService.settBryter(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, true)

        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    utbetalingKlientMock,
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
        val utbetalingKlientMock = mockk<UtbetalingKlient>(relaxed = true)
        val featureToggleService = DummyFeatureToggleService()
        featureToggleService.settBryter(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, false)

        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    utbetalingKlientMock,
                    featureToggleService,
                )
            }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakFattOgAttester(sakId, behandlingId) }
    }

    @Test
    fun `skal opprette vedtak, simulere og attestere ved uendret utbetaling`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId, Revurderingaarsak.OMREGNING)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val utbetalingKlientMock =
            mockk<UtbetalingKlient> {
                every { simuler(any()) } returns
                    mockk {
                        every { etterbetaling } returns
                            listOf(
                                mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100.00) },
                                mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(-100.00) },
                            )
                        every { tilbakekreving } returns
                            listOf(
                                mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100.00) },
                                mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(-100.00) },
                            )
                    }
            }
        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    utbetalingKlientMock,
                    DummyFeatureToggleService(),
                )
            }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
        verify { utbetalingKlientMock.simuler(behandlingId) }
    }

    @Test
    fun `skal opprette vedtak, simulere og feile fordi det finnes etterbetaling`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId, Revurderingaarsak.OMREGNING)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val utbetalingKlientMock =
            mockk<UtbetalingKlient> {
                every { simuler(any()) } returns
                    mockk {
                        every { etterbetaling } returns listOf(mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100) })
                        every { tilbakekreving } returns listOf(mockk(relaxed = true))
                    }
            }
        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    utbetalingKlientMock,
                    DummyFeatureToggleService(),
                )
            }

        inspector.sendTestMessage(melding.toJson())

        val returnertMelding = inspector.inspektør.message(0)
        returnertMelding.get(EVENT_NAME_KEY).textValue() shouldBe EventNames.FEILA.lagEventnameForType()

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { utbetalingKlientMock.simuler(behandlingId) }
        verify(exactly = 0) { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
    }

    @Test
    fun `skal opprette vedtak, simulere og feile fordi det finnes tilbakekreving`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId, Revurderingaarsak.OMREGNING)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val utbetalingKlientMock =
            mockk<UtbetalingKlient> {
                every { simuler(any()) } returns
                    mockk {
                        every { etterbetaling } returns listOf(mockk(relaxed = true))
                        every { tilbakekreving } returns
                            listOf(mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100) })
                    }
            }
        val inspector =
            TestRapid().apply {
                OpprettVedtakforespoerselRiver(
                    this,
                    vedtakServiceMock,
                    utbetalingKlientMock,
                    DummyFeatureToggleService(),
                )
            }

        inspector.sendTestMessage(melding.toJson())

        val returnertMelding = inspector.inspektør.message(0)
        returnertMelding.get(EVENT_NAME_KEY).textValue() shouldBe EventNames.FEILA.lagEventnameForType()

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { utbetalingKlientMock.simuler(behandlingId) }
        verify(exactly = 0) { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
    }
}
