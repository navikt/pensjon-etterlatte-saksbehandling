import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.database.Vedtak
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultatType
import no.nav.etterlatte.libs.common.avkorting.Endringskode
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

import java.time.LocalDateTime
import java.util.UUID

internal class VedtaksvurderingServiceTest {

    private val repositoryMock: VedtaksvurderingRepository = mockk()
    private val service = VedtaksvurderingService(repositoryMock)

    private val sakId = "5"
    private val behandlingId = UUID.randomUUID()
    private val fnr = "fnr"

    private val vedtakSomIkkeErFattet = Vedtak(
        0,
        sakId,
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
        null,
        null,)
    private val fattetVedtak = vedtakSomIkkeErFattet.copy(vedtakFattet = true)

    @Test
    fun `når avkorting lagres og vedtak finnes fra før, så skal vedtaket oppdateres`() {
        val avkorting = AvkortingsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            Endringskode.NY,
            AvkortingsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now()
        )

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns vedtakSomIkkeErFattet
        every { repositoryMock.oppdaterAvkorting(sakId, behandlingId, any()) } returns Unit
        service.lagreAvkorting(sakId, behandlingId, fnr, avkorting)
        verify { repositoryMock.oppdaterAvkorting(sakId, behandlingId, avkorting) }
    }

    @Test
    fun `når avkorting lagres og vedtak ikke finnes fra før, så skal det opprettes nytt vedtak`() {
        val avkorting = AvkortingsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            Endringskode.NY,
            AvkortingsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now()
        )

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns null
        every { repositoryMock.lagreAvkorting(sakId, behandlingId, fnr, any()) } returns Unit
        service.lagreAvkorting(sakId, behandlingId, fnr, avkorting)
        verify { repositoryMock.lagreAvkorting(sakId, behandlingId, fnr, any())  }
    }

    @Test
    fun `skal ikke lagre avkorting på fattet vedtak`() {
        val avkorting = AvkortingsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            Endringskode.NY,
            AvkortingsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now()
        )

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns fattetVedtak
        assertThrows<KanIkkeEndreFattetVedtak> { service.lagreAvkorting(sakId, behandlingId, fnr, avkorting) }
    }


    @Test
    fun `når beregning lagres og vedtak finnes fra før, så skal vedtaket oppdateres`() {
        val beregning = BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            no.nav.etterlatte.libs.common.beregning.Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now()
        )

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns vedtakSomIkkeErFattet
        every { repositoryMock.oppdaterBeregningsgrunnlag(sakId, behandlingId, any()) } returns Unit
        service.lagreBeregningsresultat(sakId, behandlingId, fnr, beregning)
        verify { repositoryMock.oppdaterBeregningsgrunnlag(sakId, behandlingId, beregning) }
    }

    @Test
    fun `når beregning lagres og vedtak ikke finnes fra før, så skal det opprettes nytt vedtak`() {
        val beregning = BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            no.nav.etterlatte.libs.common.beregning.Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now()
        )

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns null
        every { repositoryMock.lagreBeregningsresultat(sakId, behandlingId, fnr, any()) } returns Unit
        service.lagreBeregningsresultat(sakId, behandlingId, fnr, beregning)
        verify { repositoryMock.lagreBeregningsresultat(sakId, behandlingId, fnr, any())  }
    }

    @Test
    fun `skal ikke lagre beregning på fattet vedtak`() {
        val beregning = BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.GP,
            no.nav.etterlatte.libs.common.beregning.Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            emptyList(),
            LocalDateTime.now()
        )

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns fattetVedtak
        assertThrows<KanIkkeEndreFattetVedtak> { service.lagreBeregningsresultat(sakId, behandlingId, fnr, beregning) }
    }

    @Test
    fun `når vilkårsresultat lagres og vedtak finnes fra før, så skal vedtaket oppdateres`() {
        val vilkårsresultat = VilkaarResultat(
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val virkingsDato = LocalDate.now()

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns vedtakSomIkkeErFattet
        every { repositoryMock.oppdaterVilkaarsresultat(sakId, behandlingId, any()) } returns Unit
        every { repositoryMock.lagreFnr(sakId, behandlingId, fnr) } returns Unit
        every { repositoryMock.lagreDatoVirk(sakId, behandlingId, virkingsDato) } returns Unit

        service.lagreVilkaarsresultat(sakId, behandlingId, fnr, vilkårsresultat, virkingsDato)
        verify { repositoryMock.oppdaterVilkaarsresultat(sakId, behandlingId, vilkårsresultat) }
    }

    @Test
    fun `når vilkårsresultat lagres og vedtak ikke finnes fra før, så skal det opprettes nytt vedtak`() {
        val vilkårsresultat = VilkaarResultat(
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val virkingsDato = LocalDate.now()

        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns null
        every { repositoryMock.lagreVilkaarsresultat(sakId, behandlingId, fnr, any(), any()) } returns Unit
        service.lagreVilkaarsresultat(sakId, behandlingId, fnr, vilkårsresultat, virkingsDato)
        verify { repositoryMock.lagreVilkaarsresultat(sakId, behandlingId, fnr, vilkårsresultat, virkingsDato)  }
    }

    @Test
    fun `skal ikke lagre vilkårsresultat på fattet vedtak`() {
        val vilkårsresultat = VilkaarResultat(
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val virkingsDato = LocalDate.now()
        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns fattetVedtak
        assertThrows<KanIkkeEndreFattetVedtak> { service.lagreVilkaarsresultat(sakId, behandlingId, fnr, vilkårsresultat, virkingsDato) }
    }

    @Test
    fun `hent vedatak skal returnere vedtaket fra repository`() {
        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns vedtakSomIkkeErFattet
        Assertions.assertEquals(vedtakSomIkkeErFattet, service.hentVedtak(sakId, behandlingId))
    }

    @Test
    fun `hent vedatak skal returnere NULL om vedtak ikke finnes`() {
        every { repositoryMock.hentVedtak(sakId, behandlingId) } returns null
        Assertions.assertNull(service.hentVedtak(sakId, behandlingId))
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