package vedtaksvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.vedtaksvurdering.database.Vedtak
import no.nav.etterlatte.vedtaksvurdering.database.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import vilkaarsvurdering.VilkaarsvurderingTestData
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksvurderingServiceTest {

    private val repositoryMock: VedtaksvurderingRepository = mockk()
    private val beregning = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurdering = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val behandling = mockk<BehandlingKlient>(relaxed = true)
    private val service = VedtaksvurderingService(repositoryMock, beregning, vilkaarsvurdering, behandling)

    private val sakId = 2L
    private val behandlingId = UUID.randomUUID()
    private val fnr = "fnr"
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
    fun `når beregning lagres og vedtak finnes fra før, så skal vedtaket oppdateres`() {
        val beregning = BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            no.nav.etterlatte.libs.common.beregning.Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now(),
            0L
        )

        every { repositoryMock.hentVedtak(behandlingId) } returns vedtakSomIkkeErFattet
        every { repositoryMock.oppdaterBeregningsgrunnlag(behandlingId, any()) } returns Unit
        service.lagreBeregningsresultat(
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
            fnr,
            beregning
        )
        verify { repositoryMock.oppdaterBeregningsgrunnlag(behandlingId, beregning) }
    }

    @Test
    fun `når beregning lagres og vedtak ikke finnes fra før, så skal det opprettes nytt vedtak`() {
        val beregning = BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            no.nav.etterlatte.libs.common.beregning.Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now(),
            0L
        )

        every { repositoryMock.hentVedtak(behandlingId) } returns null
        every {
            repositoryMock.lagreBeregningsresultat(
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
                fnr,
                any()
            )
        } returns Unit
        service.lagreBeregningsresultat(
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
            fnr,
            beregning
        )
        verify {
            repositoryMock.lagreBeregningsresultat(
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
                fnr,
                any()
            )
        }
    }

    @Test
    fun `skal ikke lagre beregning på fattet vedtak`() {
        val beregning = BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            no.nav.etterlatte.libs.common.beregning.Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now(),
            0L
        )

        every { repositoryMock.hentVedtak(behandlingId) } returns fattetVedtak
        assertThrows<KanIkkeEndreFattetVedtak> {
            service.lagreBeregningsresultat(
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
                fnr,
                beregning
            )
        }
    }

    @Test
    fun `når vilkårsresultat lagres og vedtak finnes fra før, så skal vedtaket oppdateres`() {
        val virkingsDato = LocalDate.now()

        every { repositoryMock.hentVedtak(behandlingId) } returns vedtakSomIkkeErFattet
        every { repositoryMock.oppdaterVilkaarsresultat(sakType, behandlingId, any()) } returns Unit
        every { repositoryMock.lagreFnr(behandlingId, fnr) } returns Unit
        every { repositoryMock.lagreDatoVirk(behandlingId, virkingsDato) } returns Unit

        service.lagreVilkaarsresultat(
            sakType,
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
            fnr,
            VilkaarsvurderingTestData.oppfylt,
            virkingsDato
        )
        verify {
            repositoryMock.oppdaterVilkaarsresultat(
                sakType,
                behandlingId,
                VilkaarsvurderingTestData.oppfylt
            )
        }
    }

    @Test
    fun `når vilkårsresultat lagres og vedtak ikke finnes fra før, så skal det opprettes nytt vedtak`() {
        val virkingsDato = LocalDate.now()

        every { repositoryMock.hentVedtak(behandlingId) } returns null
        every {
            repositoryMock.lagreVilkaarsresultat(
                sakType,
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
                fnr,
                any(),
                any()
            )
        } returns Unit
        service.lagreVilkaarsresultat(
            sakType,
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
            fnr,
            VilkaarsvurderingTestData.oppfylt,
            virkingsDato
        )
        verify {
            repositoryMock.lagreVilkaarsresultat(
                sakType,
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
                fnr,
                VilkaarsvurderingTestData.oppfylt,
                virkingsDato
            )
        }
    }

    @Test
    fun `skal ikke lagre vilkårsresultat på fattet vedtak`() {
        val virkingsDato = LocalDate.now()
        every { repositoryMock.hentVedtak(behandlingId) } returns fattetVedtak
        assertThrows<KanIkkeEndreFattetVedtak> {
            service.lagreVilkaarsresultat(
                sakType,
                Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
                fnr,
                VilkaarsvurderingTestData.oppfylt,
                virkingsDato
            )
        }
    }

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

    @Test
    fun hentFellesVedtak() {
    }

    @Test
    fun fattVedtakSaksbehandler() {
    }

    @Test
    fun attesterVedtakSaksbehandler() {
    }

    @Test
    fun fattVedtak() {
    }

    @Test
    fun attesterVedtak() {
    }

    @Test
    fun utbetalingsperioderFraVedtak() {
    }
}