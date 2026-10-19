package no.nav.etterlatte.brev

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.VedtaksbrevService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerForbehandlingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerRevurderingBrevService
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrev
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevType
import no.nav.etterlatte.behandling.vedtaksvurdering.vedtak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.revurdering
import org.junit.jupiter.api.Test

internal class BrevServiceTest {
    private val behandlingMedBrevService = mockk<BehandlingMedBrevService>()
    private val behandlingService = mockk<BehandlingService>()
    private val brevApiKlient = mockk<BrevApiKlient>()
    private val vedtakInternalService = mockk<VedtakInternalService>()
    private val vedtaksbrevService = mockk<VedtaksbrevService>()

    private val service =
        BrevService(
            behandlingMedBrevService = behandlingMedBrevService,
            behandlingService = behandlingService,
            brevApiKlient = brevApiKlient,
            vedtakInternalService = vedtakInternalService,
            tilbakekrevingBrevService = mockk(relaxed = true),
            klageAvvistBrevService = mockk(relaxed = true),
            etteroppgjoerForbehandlingBrevService = mockk<EtteroppgjoerForbehandlingBrevService>(relaxed = true),
            etteroppgjoerRevurderingBrevService = mockk<EtteroppgjoerRevurderingBrevService>(relaxed = true),
            vedtaksbrevService = vedtaksbrevService,
            featureToggleService = DummyFeatureToggleService(),
        )

    private val bruker = simpleSaksbehandler("saksbehandler01")
    private val sakId = SakId(1L)

    @Test
    fun `NY_SOEKNAD-revurdering som ender i AVSLAG skal gaa gjennom gammel brevflyt`() {
        val behandling =
            revurdering(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
            )
        val vedtak =
            vedtak(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                behandlingId = behandling.id,
                type = VedtakType.AVSLAG,
            )

        every { behandlingMedBrevService.hentBehandlingMedBrev(behandling.id) } returns
            BehandlingMedBrev(behandling.id, BehandlingMedBrevType.BEHANDLING, "FATTET_VEDTAK")
        every { behandlingMedBrevService.erBehandlingRedigerbar(behandling.id) } returns true
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        coEvery { vedtakInternalService.hentVedtak(behandling.id, bruker) } returns vedtak.toDto()
        coEvery { brevApiKlient.opprettVedtaksbrev(behandling.id, sakId, bruker) } returns mockk<Brev>()

        runBlocking {
            service.opprettStrukturertBrev(behandling.id, sakId, bruker)
        }

        coVerify(exactly = 1) { brevApiKlient.opprettVedtaksbrev(behandling.id, sakId, bruker) }
        coVerify(exactly = 0) { vedtaksbrevService.opprettVedtaksbrev(any(), any()) }
    }

    @Test
    fun `NY_SOEKNAD-revurdering som ender i ENDRING (vilkaar oppfylt) skal fortsatt gaa gjennom ny brevflyt`() {
        val behandling =
            revurdering(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
            )
        val vedtak =
            vedtak(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                behandlingId = behandling.id,
                type = VedtakType.ENDRING,
            )

        every { behandlingMedBrevService.hentBehandlingMedBrev(behandling.id) } returns
            BehandlingMedBrev(behandling.id, BehandlingMedBrevType.BEHANDLING, "FATTET_VEDTAK")
        every { behandlingMedBrevService.erBehandlingRedigerbar(behandling.id) } returns true
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        coEvery { vedtakInternalService.hentVedtak(behandling.id, bruker) } returns vedtak.toDto()
        coEvery { vedtaksbrevService.opprettVedtaksbrev(behandling.id, bruker) } returns mockk<Brev>()

        runBlocking {
            service.opprettStrukturertBrev(behandling.id, sakId, bruker)
        }

        coVerify(exactly = 1) { vedtaksbrevService.opprettVedtaksbrev(behandling.id, bruker) }
        coVerify(exactly = 0) { brevApiKlient.opprettVedtaksbrev(any(), any(), any()) }
    }
}
