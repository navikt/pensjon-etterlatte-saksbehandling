package no.nav.etterlatte.inntektsjustering.selvbetjening

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.inntektsjustering.InntektsjusteringRequest
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektsjusteringSelvbetjeningServiceTest {
    private val oppgaveService: OppgaveService = mockk()
    private val rapid: KafkaProdusent<String, String> = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val vedtakKlient: VedtakKlient = mockk()

    val service =
        InntektsjusteringSelvbetjeningService(
            oppgaveService,
            behandlingService,
            vedtakKlient,
            rapid,
            featureToggleService,
        )

    @BeforeAll
    fun setup() {
        nyKontekstMedBruker(mockk())
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        every { rapid.publiser(any(), any()) } returns Pair(1, 1L)
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk()
    }

    @Test
    fun `skal behandle inntektsjustering manuelt hvis sak har aapne behandlinger`() {
        val behandlingOgSakMock = mockk<BehandlingOgSak>()
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns listOf(behandlingOgSakMock)

        val inntektsjusteringRequest = InntektsjusteringRequest(SakId(123L), "123", UUID.randomUUID(), 100, 0)
        service.behandleInntektsjustering(inntektsjusteringRequest)

        verify(exactly = 1) {
            behandlingService.hentAapneBehandlingerForSak(any())
        }

        coVerify(exactly = 0) {
            vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any())
        }

        verifyManuellBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering manuelt hvis vedtak er under samordning`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns emptyList()

        val loependeYtelseMock = mockk<LoependeYtelseDTO>(relaxed = true)
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtelseMock
        every { loependeYtelseMock.erLoepende } returns true
        every { loependeYtelseMock.underSamordning } returns true

        val inntektsjusteringRequest = InntektsjusteringRequest(SakId(123L), "123", UUID.randomUUID(), 100, 0)
        service.behandleInntektsjustering(inntektsjusteringRequest)

        coVerify(exactly = 1) {
            behandlingService.hentAapneBehandlingerForSak(any())
            vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any())
        }

        verifyManuellBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering manuelt hvis vedtak ikke er loepende`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns emptyList()

        val loependeYtelseMock = mockk<LoependeYtelseDTO>(relaxed = true)
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtelseMock
        every { loependeYtelseMock.erLoepende } returns false
        every { loependeYtelseMock.underSamordning } returns false

        val inntektsjusteringRequest = InntektsjusteringRequest(SakId(123L), "123", UUID.randomUUID(), 100, 0)
        service.behandleInntektsjustering(inntektsjusteringRequest)

        coVerify(exactly = 1) {
            behandlingService.hentAapneBehandlingerForSak(any())
            vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any())
        }

        verifyManuellBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering automatisk hvis featureToggle = true, vedtak er loepende og ikke under samordning`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns emptyList()

        val loependeYtelseMock = mockk<LoependeYtelseDTO>(relaxed = true)
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtelseMock
        every { loependeYtelseMock.erLoepende } returns true
        every { loependeYtelseMock.underSamordning } returns false

        val inntektsjusteringRequest = InntektsjusteringRequest(SakId(123L), "123", UUID.randomUUID(), 100, 0)
        service.behandleInntektsjustering(inntektsjusteringRequest)

        verifyAutomatiskBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering manuelt hvis featureToggle = false`() {
        every { featureToggleService.isEnabled(any(), any()) } returns false

        val inntektsjusteringRequest = InntektsjusteringRequest(SakId(123L), "123", UUID.randomUUID(), 100, 0)
        service.behandleInntektsjustering(inntektsjusteringRequest)

        verify(exactly = 0) {
            behandlingService wasNot Called
            vedtakKlient wasNot Called
        }

        verifyManuellBehandling()
    }

    private fun verifyManuellBehandling() {
        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                sakId = SakId(123L),
                kilde = OppgaveKilde.BRUKERDIALOG,
                type = OppgaveType.MOTTATT_INNTEKTSJUSTERING,
                merknad = "Mottatt inntektsjustering",
                referanse = "123",
                frist = null,
                saksbehandler = null,
            )
        }

        verify(exactly = 0) {
            rapid wasNot Called
        }
    }

    private fun verifyAutomatiskBehandling() {
        coVerify(exactly = 1) {
            rapid.publiser(
                "inntektsjustering-123",
                withArg {
                    // TODO: add args
                },
            )
            behandlingService.hentAapneBehandlingerForSak(any())
            vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any())
        }
        verify(exactly = 0) {
            oppgaveService wasNot Called
        }
    }
}
