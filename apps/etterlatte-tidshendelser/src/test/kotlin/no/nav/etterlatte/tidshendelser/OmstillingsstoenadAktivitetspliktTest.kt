package no.nav.etterlatte.tidshendelser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.hendelser.JobbStatus
import no.nav.etterlatte.tidshendelser.hendelser.Steg
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import no.nav.etterlatte.tidshendelser.omstillingsstoenad.OmstillingsstoenadService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth

class OmstillingsstoenadAktivitetspliktTest {
    private val behandlingKlient: BehandlingKlient = mockk<BehandlingKlient>()
    private val hendelseDao: HendelseDao = mockk<HendelseDao>()
    private val omstillingsstoenadService = OmstillingsstoenadService(hendelseDao, behandlingKlient)

    @Test
    fun `ingen saker for aktuell maaned gir ingen hendelser`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val jobb = hendelserJobb(JobbType.OMS_DOED_4MND, behandlingsmaaned)

        every { behandlingKlient.hentSakerForDoedsfall(behandlingsmaaned.minusMonths(4)) } returns emptyList()
        every { behandlingKlient.hentSaker(emptyList()) } returns emptyMap()
        every { behandlingKlient.hentSakerForPleieforholdetOpphoerte(any()) } returns emptyList()

        omstillingsstoenadService.execute(jobb)

        verify { behandlingKlient.hentSakerForDoedsfall(behandlingsmaaned.minusMonths(4)) }
        verify(exactly = 0) { hendelseDao.opprettHendelserForSaker(jobb.id, any(), any()) }
    }

    @Test
    fun `skal hente saker som skal vurderes og lagre hendelser for hver enkelt`() {
        val behandlingsmaaned = YearMonth.of(2025, Month.MARCH)
        val jobb = hendelserJobb(JobbType.OMS_DOED_4MND, behandlingsmaaned)
        val sak1 = SakId(65)
        val sak2 = SakId(22)
        val sak3 = SakId(15)
        val sakIder: List<SakId> = listOf(sak1, sak2, sak3)
        val saker =
            sakIder
                .map {
                    sak(it, SakType.OMSTILLINGSSTOENAD)
                }.associateBy { it.id }

        every { behandlingKlient.hentSakerForDoedsfall(behandlingsmaaned.minusMonths(4)) } returns sakIder
        every { behandlingKlient.hentSakerForPleieforholdetOpphoerte(any()) } returns emptyList()
        every { behandlingKlient.hentSaker(sakIder) } returns saker

        every {
            hendelseDao.opprettHendelserForSaker(
                jobb.id,
                listOf(sak1, sak2, sak3),
                Steg.IDENTIFISERT_SAK,
            )
        } returns Unit

        omstillingsstoenadService.execute(jobb)

        verify { behandlingKlient.hentSakerForDoedsfall(behandlingsmaaned.minusMonths(4)) }
        verify { hendelseDao.opprettHendelserForSaker(jobb.id, listOf(sak1, sak2, sak3), Steg.IDENTIFISERT_SAK) }
    }

    private fun hendelserJobb(
        type: JobbType,
        behandlingsmaaned: YearMonth,
    ) = HendelserJobb(
        id = 1,
        type = type,
        kjoeredato = LocalDate.now(),
        behandlingsmaaned = behandlingsmaaned,
        status = JobbStatus.NY,
        dryrun = false,
        opprettet = LocalDateTime.now(),
        endret = LocalDateTime.now(),
        versjon = 1,
    )
}
