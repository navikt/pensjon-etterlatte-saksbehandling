package behandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.domain.AutomatiskRevurdering
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingStatusServiceImplTest {
    private val behandlingDao: BehandlingDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val behandlingInfoDao: BehandlingInfoDao = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService = mockk()
    private val generellBehandlingService: GenerellBehandlingService = mockk()

    val service =
        BehandlingStatusServiceImpl(
            behandlingDao,
            behandlingService,
            behandlingInfoDao,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
        )

    @Test
    fun `Revurdering av type inntektsendring som er automatisk skal flyttes til manuell prosess hvis underkjent attestering`() {
        val revurdering =
            AutomatiskRevurdering(
                id = UUID.randomUUID(),
                sak = mockk(),
                behandlingOpprettet = LocalDateTime.now(),
                status = BehandlingStatus.FATTET_VEDTAK,
                kommerBarnetTilgode = null,
                virkningstidspunkt = null,
                boddEllerArbeidetUtlandet = null,
                soeknadMottattDato = null,
                revurderingsaarsak = Revurderingaarsak.INNTEKTSENDRING,
                revurderingInfo = null,
                kilde = Vedtaksloesning.GJENNY,
                begrunnelse = "",
                relatertBehandlingId = null,
                opphoerFraOgMed = null,
                tidligereFamiliepleier = null,
                sendeBrev = true,
                sistEndret = LocalDateTime.now(),
                utlandstilknytning = null,
            )
        val vedtaksendring =
            VedtakEndringDTO(
                sakIdOgReferanse = SakIdOgReferanse(SakId(123L), ""),
                vedtakHendelse = VedtakHendelse(123L, Tidspunkt.now(), null, null, null),
                vedtakType = VedtakType.ENDRING,
            )

        every { behandlingDao.lagreStatus(any(), any(), any()) } just runs
        every { behandlingService.registrerVedtakHendelse(any(), any(), any()) } just runs
        every { oppgaveService.tilUnderkjent(any(), any(), any()) } returns mockk()
        every { behandlingService.endreProsesstype(any(), any()) } just runs

        service.settReturnertVedtak(revurdering, vedtaksendring)

        verify { behandlingService.endreProsesstype(revurdering.id, Prosesstype.MANUELL) }
    }
}
