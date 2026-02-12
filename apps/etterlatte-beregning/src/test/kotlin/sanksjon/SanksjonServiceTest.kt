package no.nav.etterlatte.beregning.regler.sanksjon

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.beregning.regler.STANDARDSAK
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.lagreSanksjon
import no.nav.etterlatte.beregning.regler.sanksjon
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.sanksjon.SanksjonEndresFoerVirkException
import no.nav.etterlatte.sanksjon.SanksjonRepository
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class SanksjonServiceTest {
    private val sanksjonRepository: SanksjonRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val service =
        SanksjonService(
            sanksjonRepository = sanksjonRepository,
            behandlingKlient = behandlingKlient,
        )
    private val sakId = STANDARDSAK

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
            val sanksjon = lagreSanksjon(sakId = randomSakId())

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
        fun `opprett eller lagre sanksjon håndterer rare datoer innenfor måneder`() {
            val behandlingId = UUID.randomUUID()
            val forrigeBehandling = UUID.randomUUID()
            val sanksjon =
                lagreSanksjon(
                    fom = LocalDate.of(2024, Month.AUGUST, 5),
                    tom = LocalDate.of(2024, Month.AUGUST, 1),
                )

            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.REVURDERING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 3)),
                )

            every { sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon) } returns Unit
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, bruker, any()) } returns true
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(sakId, bruker) } returns
                SisteIverksatteBehandling(
                    forrigeBehandling,
                )
            every { sanksjonRepository.hentSanksjon(behandlingId) } returns null
            every { sanksjonRepository.hentSanksjon(forrigeBehandling) } returns
                listOf(
                    sanksjon(
                        fom = YearMonth.from(sanksjon.fom),
                        tom = null,
                    ),
                )

            assertDoesNotThrow {
                runBlocking { service.opprettEllerOppdaterSanksjon(behandlingId, sanksjon, bruker) }
            }
        }

        @Test
        fun `for revurderinger skal endringer av sanksjoner som starter før virk tillates kun hvis de er lik fram til virk`() {
            val behandlingId = UUID.randomUUID()
            val sakId = sakId1
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.REVURDERING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 3)),
                )

            val forrigeBehandlingId = UUID.randomUUID()
            val forrigeBehandling =
                SisteIverksatteBehandling(
                    id = forrigeBehandlingId,
                )

            val tidligereSanksjon =
                listOf(
                    sanksjon(
                        behandlingId = forrigeBehandlingId,
                        sakId = sakId,
                        fom = YearMonth.of(2024, 1),
                        tom = null,
                        type = SanksjonType.STANS,
                    ),
                )
            val eksisterendeSanksjoner =
                tidligereSanksjon.map {
                    it.copy(
                        behandlingId = behandlingId,
                        id = UUID.randomUUID(),
                    )
                }
            val lagreSanksjonGyldig =
                lagreSanksjon(
                    id = eksisterendeSanksjoner.first().id,
                    sakId = sakId,
                    fom = YearMonth.of(2024, 1).atDay(1),
                    tom = YearMonth.of(2024, 2).atEndOfMonth(),
                    beskrivelse = "De har nå oppfylt kriteriene, og ytelsen startes igjen",
                    type = SanksjonType.STANS,
                )

            every { sanksjonRepository.hentSanksjon(forrigeBehandlingId) } returns tidligereSanksjon
            every { sanksjonRepository.hentSanksjon(behandlingId) } returns eksisterendeSanksjoner
            every { sanksjonRepository.oppdaterSanksjon(any(), any()) } just runs
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(sakId, any()) } returns forrigeBehandling

            coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true

            assertDoesNotThrow {
                runBlocking {
                    service.opprettEllerOppdaterSanksjon(behandlingId, lagreSanksjonGyldig, bruker)
                }
            }

            val ugyldigSanksjonEndreType =
                lagreSanksjon(
                    id = eksisterendeSanksjoner.first().id,
                    sakId = sakId,
                    fom = YearMonth.of(2024, 1).atDay(1),
                    tom = null,
                    type = SanksjonType.BORTFALL,
                )

            val ugyldigNySanksjon =
                lagreSanksjon(
                    id = null,
                    sakId = sakId,
                    fom = YearMonth.of(2024, 1).atDay(1),
                    tom = null,
                    type = SanksjonType.STANS,
                )

            val ugyldigEndringSanksjonStoppesForTidlig =
                lagreSanksjonGyldig.copy(
                    tom = YearMonth.of(2024, 1).atEndOfMonth(),
                )

            assertThrows<SanksjonEndresFoerVirkException> {
                runBlocking {
                    service.opprettEllerOppdaterSanksjon(behandlingId, ugyldigSanksjonEndreType, bruker)
                }
            }
            assertThrows<SanksjonEndresFoerVirkException> {
                runBlocking {
                    service.opprettEllerOppdaterSanksjon(behandlingId, ugyldigNySanksjon, bruker)
                }
            }
            assertThrows<SanksjonEndresFoerVirkException> {
                runBlocking {
                    service.opprettEllerOppdaterSanksjon(behandlingId, ugyldigEndringSanksjonStoppesForTidlig, bruker)
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
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 3)),
                )

            every { sanksjonRepository.slettSanksjon(sanksjonId) } returns 1
            every { sanksjonRepository.hentSanksjonMedId(sanksjonId) } returns
                sanksjon(
                    id = sanksjonId,
                    behandlingId = behandlingId,
                    fom = behandling.virkningstidspunkt!!.dato,
                    tom = null,
                )
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, any(), any()) } returns true

            runBlocking {
                service.slettSanksjon(behandlingId, sanksjonId, bruker) shouldBe Unit
            }

            coVerify {
                sanksjonRepository.slettSanksjon(sanksjonId)
            }
        }

        @Test
        fun `skal ikke kunne slette sanksjon med fom foer virkningstidspunkt`() {
            val behandlingId = UUID.randomUUID()
            val sanksjonId = UUID.randomUUID()
            val virkningstidspunkt = YearMonth.of(2024, 2)

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunkt),
                )

            every { sanksjonRepository.hentSanksjonMedId(sanksjonId) } returns
                sanksjon(
                    id = sanksjonId,
                    behandlingId = behandlingId,
                    fom = virkningstidspunkt.minusMonths(1),
                    tom = null,
                )
            every { sanksjonRepository.slettSanksjon(sanksjonId) } returns 1
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, any(), any()) } returns true

            assertThrows<SanksjonEndresFoerVirkException> {
                runBlocking {
                    service.slettSanksjon(behandlingId, sanksjonId, bruker)
                }
            }
        }

        @Test
        fun `skal ikke kunne slette en sanksjon som ikke hører til angitt behandlingId`() {
            val behandlingId = UUID.randomUUID()
            val sanksjonId = UUID.randomUUID()
            val virkningstidspunkt = YearMonth.of(2024, 2)

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunkt),
                )

            every { sanksjonRepository.hentSanksjonMedId(sanksjonId) } returns
                sanksjon(
                    id = sanksjonId,
                    behandlingId = UUID.randomUUID(),
                    fom = virkningstidspunkt,
                    tom = null,
                )
            every { sanksjonRepository.slettSanksjon(sanksjonId) } returns 1
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            coEvery { behandlingKlient.kanBeregnes(behandlingId, any(), any()) } returns true

            assertThrows<UgyldigForespoerselException> {
                runBlocking {
                    service.slettSanksjon(behandlingId, sanksjonId, bruker)
                }
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
            every {
                sanksjonRepository.opprettSanksjon(
                    forrigeBehandlingId,
                    sakId,
                    bruker.ident,
                    sanksjon,
                )
            } returns Unit
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

        @Test
        fun `skal ikke kopiere sanksjoner hvis behandlingen allerede har sanksjoner`() {
            val behandlingId = UUID.randomUUID()
            val forrigeBehandlingId = UUID.randomUUID()
            val sanksjonerIBehandling = listOf(sanksjon(behandlingId = behandlingId))
            val sanksjonerForrige = listOf(sanksjon(behandlingId = forrigeBehandlingId))

            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    status = BehandlingStatus.BEREGNET,
                )

            every { sanksjonRepository.hentSanksjon(behandlingId) } returns sanksjonerIBehandling
            every { sanksjonRepository.hentSanksjon(forrigeBehandlingId) } returns sanksjonerForrige

            coEvery { behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, any()) } returns
                SisteIverksatteBehandling(forrigeBehandlingId)

            // Vi oppretter ingen sanksjoner, siden behandlingen har allerede sanksjoner lagt inn
            verify(exactly = 0) {
                sanksjonRepository.opprettSanksjonFraKopi(any(), any(), any())
            }
        }
    }
}
