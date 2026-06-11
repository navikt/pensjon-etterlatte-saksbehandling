package no.nav.etterlatte.avkorting.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.etteroppgjoer
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerDto
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class EtteroppgjoerServiceTest {
    private val avkortingRepository: AvkortingRepository = mockk()
    private val sanksjonService: SanksjonService = mockk()
    private val etteroppgjoerRepository: EtteroppgjoerRepository = mockk(relaxed = true)
    private val avkortingService: AvkortingService = mockk()
    private val reparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret = mockk()
    private val vedtakKlient: VedtaksvurderingKlient = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val service =
        EtteroppgjoerService(
            avkortingRepository,
            sanksjonService,
            etteroppgjoerRepository,
            avkortingService,
            reparerAarsoppgjoeret,
            vedtakKlient,
            behandlingKlient,
            featureToggleService,
        )

    private val aar = 2024
    private val sakId = SakId(1L)

    // To vedtak i etteroppgjørsåret, hvert dekker et halvår med 1000 kr/mnd = 6000 kr hver
    private val vedtakHeleAaret =
        listOf(
            VedtakEtteroppgjoerDto(
                vedtakId = 100L,
                perioder =
                    listOf(
                        VedtakEtteroppgjoerPeriode(YearMonth.of(aar, 1), YearMonth.of(aar, 6), ytelseEtterAvkorting = 1000),
                    ),
            ),
            VedtakEtteroppgjoerDto(
                vedtakId = 200L,
                perioder =
                    listOf(
                        VedtakEtteroppgjoerPeriode(YearMonth.of(aar, 7), YearMonth.of(aar, 12), ytelseEtterAvkorting = 1000),
                    ),
            ),
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    private fun mockForbehandlingMedAvkorting() {
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling(sak = sakId)

        val forbehandlingAvkorting =
            etteroppgjoer(
                aar = aar,
                avkortetYtelse =
                    listOf(
                        avkortetYtelse(
                            periode = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 12)),
                            ytelseEtterAvkorting = 1000,
                        ),
                    ),
            )
        every { avkortingRepository.hentAvkorting(any()) } returns Avkorting(aarsoppgjoer = listOf(forbehandlingAvkorting))
        every { avkortingRepository.hentFaktiskInntekt(forbehandlingAvkorting.id) } returns forbehandlingAvkorting.inntekt
    }

    @Test
    fun `uten omgjoering hentes hele vedtakslisten og det avgrenses ikke til en behandling`() {
        mockForbehandlingMedAvkorting()
        coEvery { vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(any(), any(), any(), any()) } returns vedtakHeleAaret

        val resultat =
            runBlocking {
                service.beregnOgLagreEtteroppgjoerResultat(
                    forbehandlingId = UUID.randomUUID(),
                    sisteIverksatteBehandlingId = UUID.randomUUID(),
                    etteroppgjoersAar = aar,
                    harDoedsfall = false,
                    sammenlignTilOgMedBehandlingId = null,
                )
            }

        // Begge vedtakene teller med: 6000 + 6000
        resultat.utbetaltStoenad shouldBe 12000
        resultat.referanseAvkorting.vedtakReferanse shouldBe listOf(100L, 200L)
        coVerify {
            vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(sakId, aar, any(), null)
        }
    }

    @Test
    fun `ved klage-omgjoering avgrenses vedtakslisten til og med sammenlign-behandlingen`() {
        mockForbehandlingMedAvkorting()
        val sammenlignTilOgMedBehandlingId = UUID.randomUUID()
        // Behandling-id-en vi avgrenser tidslinjen til – returnerer kun vedtaket som gjaldt før det opprinnelige etteroppgjøret
        coEvery {
            vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(any(), any(), any(), sammenlignTilOgMedBehandlingId)
        } returns vedtakHeleAaret.take(1)

        val resultat =
            runBlocking {
                service.beregnOgLagreEtteroppgjoerResultat(
                    forbehandlingId = UUID.randomUUID(),
                    sisteIverksatteBehandlingId = UUID.randomUUID(),
                    etteroppgjoersAar = aar,
                    harDoedsfall = false,
                    sammenlignTilOgMedBehandlingId = sammenlignTilOgMedBehandlingId,
                )
            }

        // Kun vedtak 100 teller med: 6000
        resultat.utbetaltStoenad shouldBe 6000
        resultat.referanseAvkorting.vedtakReferanse shouldBe listOf(100L)
        coVerify {
            vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(sakId, aar, any(), sammenlignTilOgMedBehandlingId)
        }
    }
}
