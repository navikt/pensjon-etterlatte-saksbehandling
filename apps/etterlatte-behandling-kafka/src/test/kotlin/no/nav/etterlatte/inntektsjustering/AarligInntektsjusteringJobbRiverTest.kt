package no.nav.etterlatte.inntektsjustering

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakslisteDTO
import no.nav.etterlatte.libs.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.rapidsandrivers.InntektsjusteringHendelseType
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import org.junit.jupiter.api.Test

internal class AarligInntektsjusteringJobbRiverTest {
//    private val kjoering = "inntektsjustering-jobb-2024"
//    private val loependeFom = AarligInntektsjusteringRequest.utledLoependeFom()
//
//    @Test
//    fun `teste start inntektsjustering jobb aktivert`() {
//        val featureToggleService =
//            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }
//
//        val behandlingServiceMock =
//            mockk<BehandlingService>(relaxed = true).also {
//                every { it.hentAlleSaker(any(), any(), any(), any()) } returns SakslisteDTO(listOf(randomSakId()))
//            }
//
//        val inspector =
//            TestRapid().apply { AarligInntektsjusteringJobbRiver(this, behandlingServiceMock, featureToggleService) }
//        inspector.sendTestMessage(genererMelding())
//
//        verify(exactly = 1) {
//            behandlingServiceMock.hentAlleSaker(
//                kjoering,
//                any(),
//                any(),
//                any(),
//                SakType.OMSTILLINGSSTOENAD,
//                loependeFom = loependeFom,
//            )
//        }
//        // TODO kaller behandling...
//    }
//
//    @Test
//    fun `teste start inntektsjustering jobb deaktivert`() {
//        val featureToggleService =
//            mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns false }
//
//        val behandlingServiceMock =
//            mockk<BehandlingService>(relaxed = true).also {
//                every { it.hentAlleSaker(any(), any(), any(), any()) } returns SakslisteDTO(listOf(randomSakId()))
//            }
//
//        val inspector =
//            TestRapid().apply { AarligInntektsjusteringJobbRiver(this, behandlingServiceMock, featureToggleService) }
//
//        inspector.sendTestMessage(genererMelding())
//        verify(exactly = 0) {
//            behandlingServiceMock.hentAlleSaker(
//                kjoering,
//                any(),
//                any(),
//                any(),
//                SakType.OMSTILLINGSSTOENAD,
//                loependeFom = loependeFom,
//            )
//        }
//    }
//
//    private fun genererMelding() =
//        JsonMessage
//            .newMessage(
//                mapOf(
//                    InntektsjusteringHendelseType.START_INNTEKTSJUSTERING_JOBB.lagParMedEventNameKey(),
//                    KJOERING to kjoering,
//                    ANTALL to 12000,
//                    SPESIFIKKE_SAKER to listOf<SakId>(),
//                    EKSKLUDERTE_SAKER to listOf<SakId>(),
//                ),
//            ).toJson()
}
