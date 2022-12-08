package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class BehandlingTest {

    private val behandling = Foerstegangsbehandling(
        id = UUID.randomUUID(),
        sak = 1,
        behandlingOpprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
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
        soeknadMottattDato = LocalDateTime.now(),
        gyldighetsproeving = null
    )

    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("saksbehandler01", Tidspunkt.now().instant)

    private val kommerBarnetTilgode = KommerBarnetTilgode(JaNeiVetIkke.JA, "", saksbehandler)
    private val virkningstidspunkt = Virkningstidspunkt(YearMonth.of(2021, 1), saksbehandler)
    private val gyldighetsResultat = GyldighetsResultat(VurderingsResultat.OPPFYLT, listOf(), LocalDateTime.now())

    @Test
    fun `kan oppdatere behandling når den er OPPRETTET`() {
        behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert()
            .let {
                assertEquals(kommerBarnetTilgode, it.kommerBarnetTilgode)
                assertEquals(virkningstidspunkt, it.virkningstidspunkt)
                assertEquals(BehandlingStatus.VILKAARSVURDERT, it.status)
            }
    }

    @Test
    fun `kan ikke oppdatere behandlingen når den er til attestering`() {
        val behandlingTilAttestering = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert()
            .tilFattetVedtak()

        assertThrows<TilstandException.UgyldigtTilstand> {
            behandlingTilAttestering.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    @Test
    fun `kan ikke oppdatere behandling når den er iverksatt`() {
        val iverksattBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert()
            .tilFattetVedtak()
            .tilAttestert()
            .tilIverksatt()

        assertThrows<TilstandException.UgyldigtTilstand> { iverksattBehandling.tilReturnert() }
        assertThrows<TilstandException.UgyldigtTilstand> {
            iverksattBehandling.oppdaterKommerBarnetTilgode(
                kommerBarnetTilgode
            )
        }
    }

    @Test
    fun `behandling må være fylt ut for å settes til VILKAARSVURDERING`() {
        assertThrows<TilstandException.IkkeFyltUt> { behandling.tilVilkaarsvurdert() }
    }

    @Test
    fun `behandling må være fylt ut for å settes til FATTET VEDTAK`() {
        assertThrows<TilstandException.IkkeFyltUt> { behandling.tilFattetVedtak() }
    }

    @Test
    fun `behandling kan endres igjen etter den har blitt returnet av attestant`() {
        val returnertBehandling = behandling.oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert()
            .tilFattetVedtak()
            .tilReturnert()

        val nyttVirkningstidspunkt = Virkningstidspunkt(YearMonth.of(2022, 2), saksbehandler)

        returnertBehandling
            .oppdaterVirkningstidspunkt(nyttVirkningstidspunkt.dato, nyttVirkningstidspunkt.kilde)
            .let {
                assertEquals(nyttVirkningstidspunkt, it.virkningstidspunkt)
            }
    }
}