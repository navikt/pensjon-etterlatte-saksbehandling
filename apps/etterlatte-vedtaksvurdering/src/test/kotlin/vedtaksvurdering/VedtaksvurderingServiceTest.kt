package vedtaksvurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.database.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class VedtaksvurderingServiceTest {

    private val repositoryMock: VedtaksvurderingRepository = mockk()
    private val beregning = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurdering = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val behandling = mockk<BehandlingKlient>(relaxed = true)
    private val service = VedtaksvurderingService(repositoryMock, beregning, vilkaarsvurdering, behandling, mockk())

    private val sakId = 2L
    private val behandlingId = UUID.randomUUID()
    private val sakType = SakType.BARNEPENSJON

    private val vedtakSomIkkeErFattet = Vedtak(
        0,
        sakId,
        sakType.toString(),
        behandlingId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        BehandlingType.FØRSTEGANGSBEHANDLING
    )
    private val fattetVedtak = vedtakSomIkkeErFattet.copy(vedtakFattet = true)

    @Test
    fun `hent vedatak skal returnere vedtaket fra repository`() {
        every { repositoryMock.hentVedtak(behandlingId) } returns vedtakSomIkkeErFattet
        Assertions.assertEquals(vedtakSomIkkeErFattet, service.hentVedtak(behandlingId))
    }

    @Test
    fun `hent vedatak skal returnere NULL om vedtak ikke finnes`() {
        every { repositoryMock.hentVedtak(behandlingId) } returns null
        Assertions.assertNull(service.hentVedtak(behandlingId))
    }
}