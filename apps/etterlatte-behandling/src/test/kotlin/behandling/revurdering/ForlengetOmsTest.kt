package no.nav.etterlatte.behandling.revurdering

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ForlengetOmsTest {
    private val fom = YearMonth.of(2027, 1)
    private val tom = YearMonth.of(2027, 12)

    @Test
    fun `perioden kan vaere en enkelt maaned`() {
        behandling(RevurderingInfo.ForlengetOms(fom, fom), fom.plusMonths(1)).validerForlengetOmsPeriode()
    }

    @Test
    fun `manglende periode og perioder som ikke samsvarer med behandlingen avvises`() {
        val ugyldigePerioder =
            listOf(
                null,
                RevurderingInfo.ForlengetOms(fom, fom.minusMonths(1)),
                RevurderingInfo.ForlengetOms(fom.plusMonths(1), tom),
                RevurderingInfo.ForlengetOms(fom, tom.minusMonths(1)),
            )

        ugyldigePerioder.forEach { periode ->
            shouldThrow<UgyldigForespoerselException> {
                behandling(periode).validerForlengetOmsPeriode()
            }
        }
        shouldThrow<UgyldigForespoerselException> {
            behandling(RevurderingInfo.ForlengetOms(fom, tom), null).validerForlengetOmsPeriode()
        }
    }

    @Test
    fun `lagrer eksplisitt periode og foerste maaned uten stoenad i samme operasjon`() {
        val behandling = behandling(null, null)
        val behandlingDao = mockk<BehandlingDao>(relaxed = true)
        val revurderingDao = mockk<RevurderingDao>(relaxed = true)
        every { behandlingDao.hentBehandling(behandling.id) } returns behandling
        val service = service(behandlingDao, revurderingDao)
        val info = RevurderingInfoMedBegrunnelse(RevurderingInfo.ForlengetOms(fom, tom), null)

        service.lagreRevurderingInfo(behandling.id, info, "Z123456")

        verify { behandlingDao.lagreOpphoerFom(behandling.id, tom.plusMonths(1)) }
        verify { revurderingDao.lagreRevurderingInfo(behandling.id, info, any()) }
    }

    @Test
    fun `ugyldig periode lagres ikke`() {
        val behandling = behandling(null, null)
        val behandlingDao = mockk<BehandlingDao>(relaxed = true)
        val revurderingDao = mockk<RevurderingDao>(relaxed = true)
        every { behandlingDao.hentBehandling(behandling.id) } returns behandling

        shouldThrow<UgyldigForespoerselException> {
            service(behandlingDao, revurderingDao).lagreRevurderingInfo(
                behandling.id,
                RevurderingInfoMedBegrunnelse(RevurderingInfo.ForlengetOms(fom, fom.minusMonths(1)), null),
                "Z123456",
            )
        }

        verify(exactly = 0) { behandlingDao.lagreOpphoerFom(any(), any()) }
        verify(exactly = 0) { revurderingDao.lagreRevurderingInfo(any(), any(), any()) }
    }

    @Test
    fun `perioden kan ikke endres etter at utfallet er vurdert`() {
        val behandling = behandling(RevurderingInfo.ForlengetOms(fom, tom)).tilVilkaarsvurdert()
        val behandlingDao = mockk<BehandlingDao>(relaxed = true)
        val revurderingDao = mockk<RevurderingDao>(relaxed = true)
        every { behandlingDao.hentBehandling(behandling.id) } returns behandling

        shouldThrow<UgyldigForespoerselException> {
            service(behandlingDao, revurderingDao).lagreRevurderingInfo(
                behandling.id,
                RevurderingInfoMedBegrunnelse(RevurderingInfo.ForlengetOms(fom, tom.plusMonths(1)), null),
                "Z123456",
            )
        }

        verify(exactly = 0) { behandlingDao.lagreOpphoerFom(any(), any()) }
        verify(exactly = 0) { revurderingDao.lagreRevurderingInfo(any(), any(), any()) }
    }

    private fun behandling(
        periode: RevurderingInfo.ForlengetOms?,
        opphoerFom: YearMonth? = tom.plusMonths(1),
    ) = revurdering(
        sakId = sakId1,
        sakType = SakType.OMSTILLINGSSTOENAD,
        status = BehandlingStatus.OPPRETTET,
        revurderingAarsak = Revurderingaarsak.FORLENGET_OMS,
        virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(fom),
        revurderingInfo = RevurderingInfoMedBegrunnelse(periode, null),
        opphoerFraOgMed = opphoerFom,
    )

    private fun service(
        behandlingDao: BehandlingDao,
        revurderingDao: RevurderingDao,
    ) = RevurderingService(
        oppgaveService = mockk(),
        grunnlagService = mockk(),
        behandlingHendelser = mockk(),
        behandlingDao = behandlingDao,
        hendelseDao = mockk(),
        kommerBarnetTilGodeService = mockk(),
        revurderingDao = revurderingDao,
        aktivitetspliktDao = mockk(),
        aktivitetspliktKopierService = mockk(),
    )
}
