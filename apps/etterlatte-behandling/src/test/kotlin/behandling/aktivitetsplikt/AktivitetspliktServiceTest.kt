package behandling.aktivitetsplikt

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitet
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitetType
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.aktivitetsplikt.LagreAktivitetspliktAktivitet
import no.nav.etterlatte.behandling.aktivitetsplikt.SakidTilhoererIkkeBehandlingException
import no.nav.etterlatte.behandling.aktivitetsplikt.TomErFoerFomException
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.nyKontekstMedBruker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktServiceTest {
    private val aktivitetspliktDao: AktivitetspliktDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val service = AktivitetspliktService(aktivitetspliktDao, behandlingService)
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val brukerTokenInfo =
        mockk<BrukerTokenInfo> {
            every { ident() } returns "Z999999"
        }

    @BeforeEach
    fun setup() {
        nyKontekstMedBruker(user)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class HentAktiviteter {
        @Test
        fun `Skal returnere liste med aktiviteter`() {
            val behandlingId = UUID.randomUUID()
            val aktivitet = mockk<AktivitetspliktAktivitet>()
            every { aktivitetspliktDao.hentAktiviteter(behandlingId) } returns listOf(aktivitet)

            val result = service.hentAktiviteter(behandlingId)

            result shouldBe listOf(aktivitet)
        }
    }

    @Nested
    inner class OpprettEllerOppdaterAktivitet {
        @Test
        fun `Skal opprette en aktivitet`() {
            every { aktivitetspliktDao.opprettAktivitet(behandling.id, aktivitet, any()) } returns 1

            service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)

            coVerify { aktivitetspliktDao.opprettAktivitet(behandling.id, aktivitet, any()) }
            coVerify(exactly = 0) { aktivitetspliktDao.oppdaterAktivitet(any(), any(), any()) }
        }

        @Test
        fun `Skal oppdatere en aktivitet`() {
            val aktivitet = aktivitet.copy(id = UUID.randomUUID())
            every { aktivitetspliktDao.oppdaterAktivitet(behandling.id, aktivitet, any()) } returns 1

            service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)

            coVerify { aktivitetspliktDao.oppdaterAktivitet(behandling.id, aktivitet, any()) }
            coVerify(exactly = 0) { aktivitetspliktDao.opprettAktivitet(any(), any(), any()) }
        }

        @Test
        fun `Skal kaste feil hvis sakId ikke stemmer overens med behandling`() {
            val aktivitet = aktivitet.copy(sakId = 2)

            assertThrows<SakidTilhoererIkkeBehandlingException> {
                service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal kaste feil hvis tom er foer fom`() {
            val aktivitet = aktivitet.copy(tom = LocalDate.now().minusYears(1))

            assertThrows<TomErFoerFomException> {
                service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)
            }
        }

        @Test
        fun `Skal kaste feil hvis behandlingen ikke kan endres`() {
            val behandling =
                behandling.apply {
                    every { status } returns BehandlingStatus.ATTESTERT
                }
            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            assertThrows<BehandlingKanIkkeEndres> {
                service.upsertAktivitet(behandling.id, aktivitet, brukerTokenInfo)
            }
        }
    }

    @Nested
    inner class SlettAktivitet {
        private val aktivitetId = UUID.randomUUID()

        @Test
        fun `Skal slette en aktivitet`() {
            every { aktivitetspliktDao.slettAktivitet(aktivitetId, behandling.id) } returns 1
            every { behandlingService.hentBehandling(behandling.id) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                }

            service.slettAktivitet(behandling.id, aktivitetId)

            coVerify { aktivitetspliktDao.slettAktivitet(aktivitetId, behandling.id) }
        }

        @Test
        fun `Skal kaste feil hvis behandling ikke kan endres`() {
            every { behandlingService.hentBehandling(behandling.id) } returns
                behandling.apply {
                    every { status } returns BehandlingStatus.FATTET_VEDTAK
                }

            assertThrows<BehandlingKanIkkeEndres> {
                service.slettAktivitet(behandling.id, aktivitetId)
            }
        }
    }

    companion object {
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.VILKAARSVURDERT
                every { sak } returns
                    mockk {
                        every { id } returns 1L
                    }
            }
        val aktivitet =
            LagreAktivitetspliktAktivitet(
                sakId = 1L,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )
    }
}
