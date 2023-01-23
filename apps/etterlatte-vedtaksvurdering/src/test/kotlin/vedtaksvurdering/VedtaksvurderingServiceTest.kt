package no.nav.etterlatte.vedtaksvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class VedtaksvurderingServiceTest {

    private val repositoryMock: VedtaksvurderingRepository = mockk()
    private val beregningMock = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurderingMock = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val behandlingMock = mockk<BehandlingKlient>(relaxed = true)
    private val service = VedtaksvurderingService(
        repositoryMock,
        beregningMock,
        vilkaarsvurderingMock,
        behandlingMock,
        mockk(),
        mockk()
    )

    private val sakId = 2L
    private val behandlingId = UUID.randomUUID()
    private val accessToken = "sesam sesam"

    private val vedtakSomIkkeErFattet = Vedtak(
        0,
        sakId,
        SakType.BARNEPENSJON,
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
        BehandlingType.FØRSTEGANGSBEHANDLING,
        null,
        null
    )

    @Test
    fun `hent vedtak skal returnere vedtaket fra repository`() {
        every { repositoryMock.hentVedtak(behandlingId) } returns vedtakSomIkkeErFattet
        Assertions.assertEquals(vedtakSomIkkeErFattet, service.hentVedtak(behandlingId))
    }

    @Test
    fun `hent vedtak skal returnere NULL om vedtak ikke finnes`() {
        every { repositoryMock.hentVedtak(behandlingId) } returns null
        Assertions.assertNull(service.hentVedtak(behandlingId))
    }

    @Test
    fun `hentDataForVedtak skal hente en beregning om vilkårsvurdering er oppfylt`() {
        coEvery { vilkaarsvurderingMock.hentVilkaarsvurdering(behandlingId, accessToken) } returns VilkaarsvurderingDto(
            behandlingId,
            emptyList(),
            YearMonth.now(),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, null, LocalDateTime.now(), "")
        )
        coEvery { beregningMock.hentBeregning(behandlingId, accessToken) } returns BeregningDTO(
            UUID.randomUUID(),
            behandlingId,
            emptyList(),
            Tidspunkt.now(),
            Metadata(1, 1)
        )

        runBlocking {
            service.hentDataForVedtak(behandlingId, accessToken)
            coVerify(exactly = 1) { beregningMock.hentBeregning(behandlingId, accessToken) }
        }
    }

    @Test
    fun `hentDataForVedtak skal ikke hente en beregning om vilkårsvurdering ikke er oppfylt`() {
        coEvery { vilkaarsvurderingMock.hentVilkaarsvurdering(behandlingId, accessToken) } returns VilkaarsvurderingDto(
            behandlingId,
            emptyList(),
            YearMonth.now(),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.IKKE_OPPFYLT, null, LocalDateTime.now(), "")
        )
        runBlocking {
            service.hentDataForVedtak(behandlingId, accessToken)
            coVerify(exactly = 0) { beregningMock.hentBeregning(behandlingId, accessToken) }
        }
    }

    @Test
    fun `hentDataForVedtak skal hente beregning og ikke vilkaaarsvurdering hvis behandlingstypen er MANUELT_OPPHOER`() {
        coEvery { behandlingMock.hentBehandling(any(), any()) } returns DetaljertBehandling(
            id = behandlingId,
            sak = 0,
            behandlingOpprettet = LocalDateTime.now(),
            sistEndret = LocalDateTime.now(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = null,
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            gyldighetsproeving = null,
            status = null,
            behandlingType = BehandlingType.MANUELT_OPPHOER,
            virkningstidspunkt = null,
            kommerBarnetTilgode = null,
            revurderingsaarsak = null
        )
        runBlocking {
            service.hentDataForVedtak(behandlingId, accessToken)
            coVerify(exactly = 0) { vilkaarsvurderingMock.hentVilkaarsvurdering(behandlingId, accessToken) }
            coVerify(exactly = 1) { beregningMock.hentBeregning(behandlingId, accessToken) }
        }
    }
}