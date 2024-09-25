package no.nav.etterlatte.tidshendelser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth

class OmstillingsstoenadDoedsdatoTest {
    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()
    private val behandlingKlient: BehandlingKlient = mockk<BehandlingKlient>()
    private val hendelseDao: HendelseDao = mockk<HendelseDao>()
    private val omstillingsstoenadService = OmstillingsstoenadService(hendelseDao, grunnlagKlient, behandlingKlient)

    @Test
    fun `ingen saker for aktuell maaned gir ingen hendelser`() {
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val jobb = hendelserJobb(JobbType.OMS_DOED_3AAR, behandlingsmaaned)

        every { grunnlagKlient.hentSakerForDoedsfall(behandlingsmaaned.minusYears(3)) } returns emptyList()
        every { behandlingKlient.hentSaker(emptyList()) } returns emptyMap()

        omstillingsstoenadService.execute(jobb)

        verify { grunnlagKlient.hentSakerForDoedsfall(behandlingsmaaned.minusYears(3)) }
        verify(exactly = 0) { hendelseDao.opprettHendelserForSaker(jobb.id, any(), any()) }
    }

    @Test
    fun `skal hente saker som skal vurderes og lagre hendelser for hver enkelt`() {
        val behandlingsmaaned = YearMonth.of(2025, Month.MARCH)
        val jobb = hendelserJobb(JobbType.OMS_DOED_3AAR, behandlingsmaaned)
        val sakIder: List<SakId> = listOf(sakId1, sakId2, sakId3)
        val saker = sakIder.map { sak(it, SakType.OMSTILLINGSSTOENAD) }.associateBy { it.id }

        every { grunnlagKlient.hentSakerForDoedsfall(behandlingsmaaned.minusYears(3)) } returns sakIder
        every { behandlingKlient.hentSaker(sakIder) } returns saker
        every { hendelseDao.opprettHendelserForSaker(jobb.id, listOf(sakId1, sakId2, sakId3), Steg.IDENTIFISERT_SAK) } returns Unit

        omstillingsstoenadService.execute(jobb)

        verify { grunnlagKlient.hentSakerForDoedsfall(behandlingsmaaned.minusYears(3)) }
        verify { hendelseDao.opprettHendelserForSaker(jobb.id, listOf(sakId1, sakId2, sakId3), Steg.IDENTIFISERT_SAK) }
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
