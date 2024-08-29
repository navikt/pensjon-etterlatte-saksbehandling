import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.InntektsjusteringInfobrevRiver
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import rapidsandrivers.InntektsjusteringHendelseType
import rapidsandrivers.RapidEvents.ANTALL
import rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import rapidsandrivers.RapidEvents.KJOERING
import rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER

class InntektsjusteringInfobrevRiver {
    private val kjoering = "infobrev-inntektsjustering-2025"

    @Test
    fun `kan ta imot infobrevmelding og kalle paa behandling`() {
        val featureToggleService =
            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }

        val behandlingServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(Sak("saksbehandler1", SakType.OMSTILLINGSSTOENAD, 0, "4808")),
                    )
            }

        val inspector =
            TestRapid().apply { InntektsjusteringInfobrevRiver(this, behandlingServiceMock, featureToggleService) }
        inspector.sendTestMessage(genererInfobrevMelding())

        verify(exactly = 1) {
            behandlingServiceMock.hentAlleSaker(kjoering, any(), any(), any(), SakType.OMSTILLINGSSTOENAD)
            // TODO: verify journalfoerOgDistribuer
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
                        listOf(Sak("saksbehandler1", SakType.OMSTILLINGSSTOENAD, 0, "4808")),
                    )
            }

        val inspector =
            TestRapid().apply { InntektsjusteringInfobrevRiver(this, behandlingServiceMock, featureToggleService) }

        inspector.sendTestMessage(genererInfobrevMelding())
        verify(exactly = 0) {
            behandlingServiceMock.hentAlleSaker(kjoering, any(), any(), any(), SakType.OMSTILLINGSSTOENAD)
        }
    }

    private fun genererInfobrevMelding() =
        JsonMessage
            .newMessage(
                mapOf(
                    InntektsjusteringHendelseType.SEND_INFOBREV.lagParMedEventNameKey(),
                    KJOERING to kjoering,
                    ANTALL to 12000,
                    SPESIFIKKE_SAKER to listOf<Long>(),
                    EKSKLUDERTE_SAKER to listOf(Long),
                ),
            ).toJson()
}
