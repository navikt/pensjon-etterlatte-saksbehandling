import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inntektsjustering.InntektsjusteringJobbRiver
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.InntektsjusteringHendelseType
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InntektsjusteringJobbRiverTest {
    private val kjoering = "inntektsjustering-jobb-2024"
    private val loependeFom = AarligInntektsjusteringRequest.utledLoependeFom()

    @Test
    fun `teste start inntektsjustering jobb aktivert`() {
        val featureToggleService =
            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }

        val behandlingServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(
                            Sak(
                                "saksbehandler1",
                                SakType.OMSTILLINGSSTOENAD,
                                randomSakId(),
                                Enheter.PORSGRUNN.enhetNr,
                            ),
                        ),
                    )
            }

        val inspector =
            TestRapid().apply { InntektsjusteringJobbRiver(this, behandlingServiceMock, featureToggleService) }
        inspector.sendTestMessage(genererMelding(loependeFom))

        verify(exactly = 1) {
            behandlingServiceMock.hentAlleSaker(
                kjoering,
                any(),
                any(),
                any(),
                SakType.OMSTILLINGSSTOENAD,
                loependeFom = loependeFom,
            )
        }
        // TODO kaller behandling...
    }

    @Test
    fun `teste start inntektsjustering jobb deaktivert`() {
        val featureToggleService =
            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns false }

        val behandlingServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(
                            Sak(
                                "saksbehandler1",
                                SakType.OMSTILLINGSSTOENAD,
                                randomSakId(),
                                Enheter.PORSGRUNN.enhetNr,
                            ),
                        ),
                    )
            }

        val inspector =
            TestRapid().apply { InntektsjusteringJobbRiver(this, behandlingServiceMock, featureToggleService) }

        inspector.sendTestMessage(genererMelding(loependeFom))
        verify(exactly = 0) {
            behandlingServiceMock.hentAlleSaker(
                kjoering,
                any(),
                any(),
                any(),
                SakType.OMSTILLINGSSTOENAD,
                loependeFom = loependeFom,
            )
        }
    }

    private fun genererMelding(loependeFom: YearMonth) =
        JsonMessage
            .newMessage(
                mapOf(
                    InntektsjusteringHendelseType.START_INNTEKTSJUSTERING_JOBB.lagParMedEventNameKey(),
                    KJOERING to kjoering,
                    ANTALL to 12000,
                    SPESIFIKKE_SAKER to listOf<SakId>(),
                    EKSKLUDERTE_SAKER to listOf<SakId>(),
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
