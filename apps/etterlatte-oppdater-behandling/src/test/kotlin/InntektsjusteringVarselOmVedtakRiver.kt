import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inntektsjustering.InntektsjusteringVarselOmVedtakRiver
import no.nav.etterlatte.inntektsjustering.skalHaVarselOmVedtak
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.InntektsjusteringHendelseType
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.LOEPENDE_FOM
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InntektsjusteringVarselOmVedtakRiver {
    private val kjoering = "infobrev-inntektsjustering-2025"
    private val loependeFom = YearMonth.of(2024, 1)

    @Test
    fun `skal ha varsel om vedtak`() {
        val behandlinger =
            listOf(
                genererBehandlingSammendrag(BehandlingStatus.IVERKSATT, Revurderingaarsak.INNTEKTSENDRING, YearMonth.of(2023, 1)),
                genererBehandlingSammendrag(BehandlingStatus.IVERKSATT, Revurderingaarsak.ANNEN, YearMonth.of(2024, 1)),
                genererBehandlingSammendrag(BehandlingStatus.AVBRUTT, Revurderingaarsak.INNTEKTSENDRING, YearMonth.of(2024, 1)),
            )
        val skalHaVarselOmVedtakResult = skalHaVarselOmVedtak(behandlinger, YearMonth.of(2024, 1))
        skalHaVarselOmVedtakResult shouldBe true
    }

    @Test
    fun `skal IKKE ha varsel om vedtak`() {
        val behandlinger =
            listOf(genererBehandlingSammendrag(BehandlingStatus.IVERKSATT, Revurderingaarsak.INNTEKTSENDRING, YearMonth.of(2024, 1)))
        val skalHaVarselOmVedtakResult = skalHaVarselOmVedtak(behandlinger, YearMonth.of(2024, 1))
        skalHaVarselOmVedtakResult shouldBe false
    }

    @Test
    fun `kan ta imot infobrevmelding og kalle paa behandling`() {
        val featureToggleService =
            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }

        val behandlingServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(Sak("saksbehandler1", SakType.OMSTILLINGSSTOENAD, 0, Enheter.PORSGRUNN.enhetNr)),
                    )
            }

        val inspector =
            TestRapid().apply { InntektsjusteringVarselOmVedtakRiver(this, behandlingServiceMock, featureToggleService) }
        inspector.sendTestMessage(genererInfobrevMelding(loependeFom))

        verify(exactly = 1) {
            behandlingServiceMock.hentAlleSaker(kjoering, any(), any(), any(), SakType.OMSTILLINGSSTOENAD, loependeFom = loependeFom)
        }
    }

    @Test
    fun `teste featureToggle deaktivert`() {
        val featureToggleService =
            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns false }

        val behandlingServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(Sak("saksbehandler1", SakType.OMSTILLINGSSTOENAD, 0, Enheter.PORSGRUNN.enhetNr)),
                    )
            }

        val inspector =
            TestRapid().apply { InntektsjusteringVarselOmVedtakRiver(this, behandlingServiceMock, featureToggleService) }

        inspector.sendTestMessage(genererInfobrevMelding(loependeFom))
        verify(exactly = 0) {
            behandlingServiceMock.hentAlleSaker(kjoering, any(), any(), any(), SakType.OMSTILLINGSSTOENAD, loependeFom = loependeFom)
        }
    }

    private fun genererInfobrevMelding(loependeFom: YearMonth) =
        JsonMessage
            .newMessage(
                mapOf(
                    InntektsjusteringHendelseType.SEND_INFOBREV.lagParMedEventNameKey(),
                    KJOERING to kjoering,
                    ANTALL to 12000,
                    SPESIFIKKE_SAKER to listOf<Long>(),
                    EKSKLUDERTE_SAKER to listOf(Long),
                    LOEPENDE_FOM to loependeFom,
                ),
            ).toJson()

    private fun genererBehandlingSammendrag(
        status: BehandlingStatus,
        aarsak: Revurderingaarsak,
        virkningstidspunkt: YearMonth,
    ): BehandlingSammendrag {
        val virkningstidspunktMock = mockk<Virkningstidspunkt>()
        every { virkningstidspunktMock.dato } returns virkningstidspunkt
        val behandlingSammendragMock = mockk<BehandlingSammendrag>()
        every { behandlingSammendragMock.status } returns status
        every { behandlingSammendragMock.aarsak } returns aarsak.name
        every { behandlingSammendragMock.virkningstidspunkt } returns virkningstidspunktMock
        return behandlingSammendragMock
    }
}
