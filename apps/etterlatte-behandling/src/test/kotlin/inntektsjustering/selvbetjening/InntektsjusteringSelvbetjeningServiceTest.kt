package no.nav.etterlatte.inntektsjustering.selvbetjening

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektsjusteringSelvbetjeningServiceTest {
    private val oppgaveService: OppgaveService = mockk()
    private val rapid: KafkaProdusent<String, String> = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val beregningKlient: BeregningKlient = mockk()
    private val vedtakKlient: VedtakKlient = mockk()

    val service =
        InntektsjusteringSelvbetjeningService(
            oppgaveService,
            behandlingService,
            beregningKlient,
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

        val mottattInntektsjustering =
            MottattInntektsjustering(
                SakId(123L),
                UUID.randomUUID(),
                "123",
                LocalDateTime.now(),
                2025,
                100,
                100,
                100,
                100,
                YearMonth.of(2025, 1),
            )
        runBlocking {
            service.behandleInntektsjustering(mottattInntektsjustering)
        }

        verify(exactly = 1) {
            behandlingService.hentAapneBehandlingerForSak(any())
        }

        coVerify(exactly = 0) {
            vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any())
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(any(), any(), any(), any())
        }

        verifyManuellBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering manuelt hvis sanksjon`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns emptyList()

        val loependeYtelseMock = mockk<LoependeYtelseDTO>(relaxed = true)
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtelseMock
        every { loependeYtelseMock.erLoepende } returns true
        every { loependeYtelseMock.underSamordning } returns false

        val inntektsjusteringAvkortingInfo = mockk<InntektsjusteringAvkortingInfoResponse>(relaxed = true)
        coEvery {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                any(),
                any(),
                any(),
                any(),
            )
        } returns inntektsjusteringAvkortingInfo
        every { inntektsjusteringAvkortingInfo.harSanksjon } returns true

        val mottattInntektsjustering =
            MottattInntektsjustering(
                SakId(123L),
                UUID.randomUUID(),
                "123",
                LocalDateTime.now(),
                2025,
                100,
                100,
                100,
                100,
                YearMonth.of(2025, 1),
            )
        runBlocking {
            service.behandleInntektsjustering(mottattInntektsjustering)
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

        val mottattInntektsjustering =
            MottattInntektsjustering(
                SakId(123L),
                UUID.randomUUID(),
                "123",
                LocalDateTime.now(),
                2025,
                100,
                100,
                100,
                100,
                YearMonth.of(2025, 1),
            )
        runBlocking {
            service.behandleInntektsjustering(mottattInntektsjustering)
        }

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

        val mottattInntektsjustering =
            MottattInntektsjustering(
                SakId(123L),
                UUID.randomUUID(),
                "123",
                LocalDateTime.now(),
                2025,
                100,
                100,
                100,
                100,
                YearMonth.of(2025, 1),
            )
        runBlocking {
            service.behandleInntektsjustering(mottattInntektsjustering)
        }

        coVerify(exactly = 1) {
            behandlingService.hentAapneBehandlingerForSak(any())
            vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any())
        }

        verifyManuellBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering automatisk hvis featureToggle = true`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns emptyList()

        val loependeYtelseMock = mockk<LoependeYtelseDTO>(relaxed = true)
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtelseMock
        every { loependeYtelseMock.erLoepende } returns true
        every { loependeYtelseMock.underSamordning } returns false

        val inntektsjusteringAvkortingInfo = mockk<InntektsjusteringAvkortingInfoResponse>(relaxed = true)
        coEvery {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                any(),
                any(),
                any(),
                any(),
            )
        } returns inntektsjusteringAvkortingInfo
        every { inntektsjusteringAvkortingInfo.harSanksjon } returns false

        val mottattInntektsjustering =
            MottattInntektsjustering(
                SakId(123L),
                UUID.randomUUID(),
                "123",
                LocalDateTime.now(),
                2025,
                100,
                100,
                100,
                100,
                YearMonth.of(2025, 1),
            )
        runBlocking {
            service.behandleInntektsjustering(mottattInntektsjustering)
        }

        verifyAutomatiskBehandling()
    }

    @Test
    fun `skal behandle inntektsjustering manuelt hvis featureToggle = false`() {
        every { featureToggleService.isEnabled(any(), any()) } returns false

        val mottattInntektsjustering =
            MottattInntektsjustering(
                SakId(123L),
                UUID.randomUUID(),
                "123",
                LocalDateTime.now(),
                2025,
                100,
                100,
                100,
                100,
                YearMonth.of(2025, 1),
            )
        runBlocking {
            service.behandleInntektsjustering(mottattInntektsjustering)
        }

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
        coVerify(exactly = 1) {
            rapid.publiser("mottak-inntektsjustering-fullfoert-123", any())
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
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(any(), any(), any(), any())
            rapid.publiser("mottak-inntektsjustering-fullfoert-123", any())
        }
    }
}
