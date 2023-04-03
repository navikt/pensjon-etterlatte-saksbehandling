package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.*

internal class BehandlingTest {

    private val behandling = Foerstegangsbehandling(
        id = UUID.randomUUID(),
        sak = Sak(
            ident = "",
            sakType = SakType.BARNEPENSJON,
            id = 1
        ),
        behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
        sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        status = BehandlingStatus.OPPRETTET,
        persongalleri = Persongalleri(
            soeker = "",
            innsender = null,
            soesken = listOf(),
            avdoed = listOf(),
            gjenlevende = listOf()
        ),
        kommerBarnetTilgode = null,
        virkningstidspunkt = null,
        soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
        gyldighetsproeving = null,
        vilkaarUtfall = null
    )

    private val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("saksbehandler01")

    private val kommerBarnetTilgode = KommerBarnetTilgode(JaNei.JA, "", saksbehandler)
    private val virkningstidspunkt = Virkningstidspunkt(YearMonth.of(2021, 1), saksbehandler, "begrunnelse")
    private val gyldighetsResultat = GyldighetsResultat(
        VurderingsResultat.OPPFYLT,
        listOf(),
        Tidspunkt.now().toLocalDatetimeUTC()
    )

    @Test
    fun `kan oppdatere behandling når den er OPPRETTET`() {
        behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .let {
                assertEquals(kommerBarnetTilgode, it.kommerBarnetTilgode)
                assertEquals(virkningstidspunkt, it.virkningstidspunkt)
                assertEquals(gyldighetsResultat, it.gyldighetsproeving)
            }
    }

    @Test
    fun `kan ikke oppdatere behandlingen når den er til attestering`() {
        val behandlingTilAttestering = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()

        assertThrows<TilstandException.UgyldigTilstand> {
            behandlingTilAttestering.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    @Test
    fun `kan ikke oppdatere behandling når den er iverksatt`() {
        val iverksattBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()
            .tilAttestert()
            .tilIverksatt()

        assertThrows<TilstandException.UgyldigTilstand> { iverksattBehandling.tilReturnert() }
        assertThrows<TilstandException.UgyldigTilstand> {
            iverksattBehandling.oppdaterKommerBarnetTilgode(
                kommerBarnetTilgode
            )
        }
    }

    @Test
    fun `behandling må være fylt ut for å settes til VILKAARSVURDERING`() {
        assertThrows<TilstandException.IkkeFyltUt> { behandling.tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT) }
    }

    @Test
    fun `behandling må være fylt ut for å settes til FATTET VEDTAK`() {
        assertThrows<TilstandException.IkkeFyltUt> { behandling.tilFattetVedtak() }
    }

    @Test
    fun `kan ikke ga fra BEREGNET til FATTET VEDTAK uten oppfylt vilkaarsutfall`() {
        assertThrows<Exception> {
            behandling.tilVilkaarsvurdert(null).tilBeregnet().tilFattetVedtak()
        }
    }

    @Test
    fun `kan ikke ga fra VILKAARSVURDERT til FATTET VEDTAK`() {
        val fyltUtBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)

        assertThrows<TilstandException.UgyldigTilstand> { fyltUtBehandling.tilVilkaarsvurdert(null).tilFattetVedtak() }
        assertThrows<TilstandException.UgyldigTilstand> {
            fyltUtBehandling.tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT).tilFattetVedtak()
        }
        assertThrows<TilstandException.UgyldigTilstand> {
            fyltUtBehandling.tilVilkaarsvurdert(VilkaarsvurderingUtfall.IKKE_OPPFYLT).tilFattetVedtak()
        }
    }

    @Test
    fun `behandling kan endres igjen etter den har blitt returnet av attestant`() {
        val returnertBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()
            .tilReturnert()

        val nyttVirkningstidspunkt = Virkningstidspunkt(YearMonth.of(2022, 2), saksbehandler, "begrunnelse")

        returnertBehandling
            .oppdaterVirkningstidspunkt(nyttVirkningstidspunkt.dato, nyttVirkningstidspunkt.kilde, "begrunnelse")
            .let {
                assertEquals(nyttVirkningstidspunkt, it.virkningstidspunkt)
            }
    }

    @Test
    fun `man maa gjennom hele loeypen paa nytt dersom man gjoer operasjoner i tidligere steg`() {
        val vilkaarsvurdertBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
        val nyttVirkningstidspunkt = Virkningstidspunkt(YearMonth.of(2022, 2), saksbehandler, "begrunnelse")

        vilkaarsvurdertBehandling
            .oppdaterVirkningstidspunkt(nyttVirkningstidspunkt.dato, nyttVirkningstidspunkt.kilde, "begrunnelse")
            .let {
                assertEquals(it.status, BehandlingStatus.OPPRETTET)
            }
    }

    @Test
    fun `kan gaa fra RETURNERT til alle redigerbare states`() {
        val initialBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()

        initialBehandling.tilReturnert().tilFattetVedtak()
        initialBehandling.tilReturnert().tilBeregnet()
        initialBehandling.tilReturnert().tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
        initialBehandling.tilReturnert().tilOpprettet()
    }

    @Test
    fun `kan ikke gaa fra RETURNERT til ATTESTERT`() {
        val behandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()
            .tilReturnert()

        assertThrows<TilstandException.UgyldigTilstand> {
            behandling.tilAttestert()
        }
    }

    @Test
    fun `kan ikke gaa fra RETURNERT til IVERKSATT`() {
        val behandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "begrunnelse")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .tilBeregnet()
            .tilFattetVedtak()
            .tilReturnert()

        assertThrows<TilstandException.UgyldigTilstand> {
            behandling.tilIverksatt()
        }
    }
}