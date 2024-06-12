package no.nav.etterlatte.beregning.regler.sanksjon

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.lagreSanksjon
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.sanksjon.Sanksjon
import no.nav.etterlatte.sanksjon.SanksjonRepository
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class SanksjonServiceTest {
    private val sanksjonRepository: SanksjonRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
    private val service =
        SanksjonService(
            sanksjonRepository = sanksjonRepository,
            behandlingKlient = behandlingKlient,
            featureToggleService = featureToggleService,
        )
    private val sakId = 123L

    @Nested
    inner class HentSanksjon {
        @Test
        fun `Skal returnere null hvis det ikke finnes sanksjoner`() {
            val behandlingId = UUID.randomUUID()
            every { sanksjonRepository.hentSanksjon(behandlingId) } returns null

            service.hentSanksjon(behandlingId) shouldBe null

            coVerify {
                sanksjonRepository.hentSanksjon(behandlingId)
            }
        }

        @Test
        fun `Skal returnere liste med Sanksjon hvis det finnes sanksjoner`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon = mockk<Sanksjon>()

            every { sanksjonRepository.hentSanksjon(behandlingId) } returns listOf(sanksjon)

            service.hentSanksjon(behandlingId) shouldBe listOf(sanksjon)

            coVerify {
                sanksjonRepository.hentSanksjon(behandlingId)
            }
        }
    }

    @Nested
    inner class OpprettEllerOppdaterSanksjon {
        @Test
        fun `Skal opprette en sanksjon`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon = lagreSanksjon()

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, any(), any()) } returns true

            runBlocking {
                service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) shouldBe Unit
            }

            coVerify {
                sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon)
            }
        }

        @Test
        fun `Skal oppdatere en sanksjon`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon = lagreSanksjon(id = UUID.randomUUID())

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.oppdaterSanksjon(sanksjon, bruker.ident) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, any(), any()) } returns true

            runBlocking {
                service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) shouldBe Unit
            }

            coVerify {
                sanksjonRepository.oppdaterSanksjon(sanksjon, bruker.ident)
            }
        }

        @Test
        fun `Feil sak id skal gi feilmelding`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon = lagreSanksjon(sakId = 321)

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

            runBlocking {
                assertThrows<Exception> {
                    service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) shouldBe Unit
                }
            }
        }

        @Test
        fun `Til og med kan ikke være før fra og med`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon =
                lagreSanksjon(
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 1, 1),
                )

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

            runBlocking {
                assertThrows<Exception> {
                    service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) shouldBe Unit
                }
            }
        }

        @Test
        fun `Fra og med kan ikke være før virkningstidspunkt`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon = lagreSanksjon()

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 3)),
                )

            every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

            runBlocking {
                assertThrows<Exception> {
                    service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) shouldBe Unit
                }
            }
        }

        @Test
        fun `Virkningstidspunkt må være satt`() {
            val behandlingId = UUID.randomUUID()
            val sanksjon = lagreSanksjon()

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt = null,
                )

            every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

            runBlocking {
                assertThrows<Exception> {
                    service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) shouldBe Unit
                }
            }
        }
    }

    @Nested
    inner class SlettSanksjon {
        @Test
        fun `Skal returnere 1 hvis en sanksjon slettes`() {
            val behandlingId = UUID.randomUUID()
            val sanksjonId = UUID.randomUUID()

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.slettSanksjon(sanksjonId) } returns 1
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, any(), any()) } returns true

            runBlocking {
                service.slettSanksjon(behandlingId, sanksjonId, bruker) shouldBe Unit
            }

            coVerify {
                sanksjonRepository.slettSanksjon(sanksjonId)
            }
        }
    }

    @Nested
    inner class KopierSanksjon {
        @Test
        fun `Skal kopiere sanksjoner fra forrige behandling`() {
            val behandlingId = UUID.randomUUID()
            val forrigeBehandlingId = UUID.randomUUID()
            val sanksjon = lagreSanksjon()
            val sanksjoner = mockk<Sanksjon>()

            val forrigeBehandling =
                behandling(
                    id = forrigeBehandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.hentSanksjon(behandlingId) } returns listOf(sanksjoner)
            every { sanksjonRepository.hentSanksjon(forrigeBehandlingId) } returns null
            every { sanksjonRepository.opprettSanksjon(forrigeBehandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.kanBeregnes(forrigeBehandlingId, any(), any()) } returns true
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.hentBehandling(forrigeBehandlingId, bruker) } returns forrigeBehandling
            coEvery {
                behandlingKlient.hentSisteIverksatteBehandling(
                    behandling.sak,
                    bruker,
                )
            } returns SisteIverksatteBehandling(forrigeBehandlingId)

            runBlocking {
                service.opprettEllerOppdaterSanksjon(forrigeBehandlingId, sanksjon, bruker)
            }

            runBlocking {
                service.kopierSanksjon(behandlingId, bruker) shouldBe Unit
            }

            service.hentSanksjon(behandlingId) shouldBe listOf(sanksjoner)

            coVerify {
                sanksjonRepository.hentSanksjon(forrigeBehandlingId)
            }
        }
    }
}
