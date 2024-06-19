package behandling.aktivitetsplikt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitetType
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.aktivitetsplikt.LagreAktivitetspliktAktivitet
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.nyKontekstMedBruker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktKopierServiceTest {
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao = mockk()
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val service =
        AktivitetspliktKopierService(
            aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao,
        )
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeEach
    fun setup() {
        nyKontekstMedBruker(user)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class KopierVurderingForSak {
        private val behandlingId = UUID.randomUUID()
        private val sakId = 1L

        @Test
        fun `Skal kopiere vurdering med unntak`() {
            val unntakId = UUID.randomUUID()
            val unntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = null,
                    tom = LocalDate.now().plusMonths(6),
                )

            every { aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, any(), null, behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns null
            every { aktivitetspliktUnntakDao.kopierUnntak(unntakId, behandlingId) } returns 1
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns null
            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns null
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns
                mockk {
                    every { id } returns unntakId
                    every { tom } returns LocalDate.now().minusYears(1)
                    every { opprettet } returns Grunnlagsopplysning.Saksbehandler.create("Z123455")
                    every { sakId } returns aktivitet.sakId
                }
            every { behandlingService.hentBehandling(behandlingId) } returns behandling

            service.kopierVurdering(sakId, behandlingId)

            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
            verify { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) }
            verify { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) }
            verify { aktivitetspliktUnntakDao.kopierUnntak(unntakId, behandlingId) }
        }

        @Test
        fun `Skal kopiere vurdering med aktivitetsgrad`() {
            val aktivitetsgradId = UUID.randomUUID()

            every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) } returns
                mockk {
                    every { id } returns aktivitetsgradId
                    every { aktivitetsgrad } returns AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
                    every { fom } returns aktivitet.fom.minusMonths(1)
                    every { sakId } returns aktivitet.sakId
                }
            every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) } returns null
            every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId) } returns null
            every { aktivitetspliktAktivitetsgradDao.kopierAktivitetsgrad(aktivitetsgradId, behandlingId) } returns 1
            every { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) } returns null
            every { behandlingService.hentBehandling(behandlingId) } returns behandling

            service.kopierVurdering(sakId, behandlingId)

            verify { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId) }
            verify { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(aktivitet.sakId) }
            verify { aktivitetspliktUnntakDao.hentNyesteUnntak(aktivitet.sakId) }
            verify { aktivitetspliktAktivitetsgradDao.kopierAktivitetsgrad(aktivitetsgradId, behandlingId) }
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
