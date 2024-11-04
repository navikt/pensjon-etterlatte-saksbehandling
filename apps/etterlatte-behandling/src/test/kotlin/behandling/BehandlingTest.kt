package no.nav.etterlatte.behandling

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID

internal class BehandlingTest {
    val id = UUID.randomUUID()
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("saksbehandler01")

    private val behandling = opprettFoerstegangsbehandling(false)

    private val virkningstidspunkt = Virkningstidspunkt(YearMonth.of(2021, 1), saksbehandler, "begrunnelse")
    private val gyldighetsResultat =
        GyldighetsResultat(
            VurderingsResultat.OPPFYLT,
            listOf(),
            Tidspunkt.now().toLocalDatetimeUTC(),
        )

    @Test
    fun `erSluttbehandling() skal ta hensyn til parameter erSluttbehandling`() {
        opprettFoerstegangsbehandling(true).erSluttbehandling() shouldBe true
        opprettFoerstegangsbehandling(false).erSluttbehandling() shouldBe false
    }

    @Test
    fun `kan oppdatere behandling når den er OPPRETTET`() {
        behandling
            .oppdaterVirkningstidspunkt(virkningstidspunkt)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .let {
                assertEquals(virkningstidspunkt, it.virkningstidspunkt)
                assertEquals(gyldighetsResultat, it.gyldighetsproeving)
            }
    }

    @Test
    fun `kan ikke oppdatere behandlingen når den er til attestering`() {
        val behandlingTilAttestering =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()

        assertThrows<TilstandException.KanIkkeRedigere> {
            behandlingTilAttestering.oppdaterVirkningstidspunkt(virkningstidspunkt)
        }
    }

    @Test
    fun `kan ikke oppdatere behandling når den er iverksatt`() {
        val iverksattBehandling =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()
                .tilAttestert()
                .tilIverksatt()

        assertThrows<TilstandException.UgyldigTilstand> { iverksattBehandling.tilReturnert() }
        assertThrows<TilstandException.KanIkkeRedigere> {
            iverksattBehandling.oppdaterVirkningstidspunkt(
                virkningstidspunkt,
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
        val returnertBehandling =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()
                .tilReturnert()

        val nyttVirkningstidspunkt = Virkningstidspunkt(YearMonth.of(2022, 2), saksbehandler, "begrunnelse")

        returnertBehandling
            .oppdaterVirkningstidspunkt(nyttVirkningstidspunkt)
            .let {
                assertEquals(nyttVirkningstidspunkt, it.virkningstidspunkt)
            }
    }

    @Test
    fun `man maa gjennom hele loeypen paa nytt dersom man gjoer operasjoner i tidligere steg`() {
        val vilkaarsvurdertBehandling =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
        val nyttVirkningstidspunkt = Virkningstidspunkt(YearMonth.of(2022, 2), saksbehandler, "begrunnelse")

        vilkaarsvurdertBehandling
            .oppdaterVirkningstidspunkt(nyttVirkningstidspunkt)
            .let {
                assertEquals(it.status, BehandlingStatus.OPPRETTET)
            }
    }

    @Test
    fun `kan gaa fra RETURNERT til alle redigerbare states`() {
        val initialBehandling =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()

        initialBehandling.tilReturnert().tilFattetVedtak()
        initialBehandling.tilReturnert().tilBeregnet()
        initialBehandling.tilReturnert().tilVilkaarsvurdert()
        initialBehandling.tilReturnert().tilTrygdetidOppdatert()
        initialBehandling.tilReturnert().tilOpprettet()
    }

    @Test
    fun `kan ikke gaa fra RETURNERT til ATTESTERT`() {
        val behandling =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()
                .tilReturnert()

        assertThrows<TilstandException.UgyldigTilstand> {
            behandling.tilAttestert()
        }
    }

    @Test
    fun `kan ikke gaa fra RETURNERT til IVERKSATT`() {
        val behandling =
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilTrygdetidOppdatert()
                .tilBeregnet()
                .tilFattetVedtak()
                .tilReturnert()

        assertThrows<TilstandException.UgyldigTilstand> {
            behandling.tilIverksatt()
        }
    }

    @Test
    fun `kan gaa fra VILKAARSVURDERT til TRYGDETID_OPPRETTET`() {
        behandling
            .oppdaterVirkningstidspunkt(virkningstidspunkt)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert()
            .tilTrygdetidOppdatert()
            .tilBeregnet()
    }

    @Test
    fun `kan ikke gaa fra VILKAARSVURDERT til BEREGNET`() {
        assertThrows<TilstandException.UgyldigTilstand> {
            behandling
                .oppdaterVirkningstidspunkt(virkningstidspunkt)
                .oppdaterGyldighetsproeving(gyldighetsResultat)
                .tilVilkaarsvurdert()
                .tilBeregnet()
        }
    }

    private fun opprettFoerstegangsbehandling(erSluttbehandling: Boolean): Foerstegangsbehandling =
        Foerstegangsbehandling(
            id = id,
            sak =
                Sak(
                    ident = "",
                    sakType = SakType.BARNEPENSJON,
                    id = sakId1,
                    enhet = Enheter.defaultEnhet.enhetNr,
                ),
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            status = BehandlingStatus.OPPRETTET,
            utlandstilknytning = Utlandstilknytning(UtlandstilknytningType.NASJONAL, saksbehandler, ""),
            kommerBarnetTilgode = KommerBarnetTilgode(JaNei.JA, "", saksbehandler, id),
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            kilde = Vedtaksloesning.GJENNY,
            sendeBrev = true,
            erSluttbehandling = erSluttbehandling,
        )
}
