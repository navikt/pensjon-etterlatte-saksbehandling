package no.nav.etterlatte.behandling.domain

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.kommerBarnetTilGodeVurdering
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.virkningstidspunktVurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class RevurderingTest {
    @Test
    fun `erSluttbehandling() skal ta hensyn til revurderingsaarsak SLUTTBEHANDLING_UTLAND`() {
        opprettetRevurdering(Prosesstype.MANUELL, Revurderingaarsak.SLUTTBEHANDLING_UTLAND).erSluttbehandling() shouldBe true
        opprettetRevurdering(Prosesstype.MANUELL, Revurderingaarsak.REGULERING).erSluttbehandling() shouldBe false
    }

    @Test
    fun `regulering kan endre tilstander`() {
        val id = UUID.randomUUID()
        Revurdering
            .opprett(
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
                kommerBarnetTilgode = kommerBarnetTilGodeVurdering(id),
                virkningstidspunkt = virkningstidspunktVurdering(),
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                revurderingsaarsak = Revurderingaarsak.REGULERING,
                prosesstype = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                revurderingInfo = null,
                begrunnelse = null,
                relatertBehandlingId = null,
                sendeBrev = true,
            ).tilVilkaarsvurdert()
            .tilTrygdetidOppdatert()
            .tilBeregnet()
            .tilVilkaarsvurdert()
            .tilTrygdetidOppdatert()
            .tilBeregnet()
            .tilFattetVedtak()
            .tilAttestert()
            .tilIverksatt()
    }

    @Nested
    inner class ManuellStatusEndring {
        private val revurdering = opprettetRevurdering(Prosesstype.MANUELL, Revurderingaarsak.REGULERING)

        @Test
        fun `kan endre status gjennom gyldig statusendringsflyt`() {
            val actual =
                revurdering
                    .tilVilkaarsvurdert()
                    .tilTrygdetidOppdatert()
                    .tilBeregnet()
                    .tilAvkortet()
                    .tilBeregnet()
                    .tilAvkortet()
                    .tilFattetVedtak()
                    .tilAttestert()
                    .tilIverksatt()

            Assertions.assertEquals(BehandlingStatus.IVERKSATT, actual.status)
        }

        @Test
        fun `kan endre status gjennom gyldig statusendringsflyt - samordning`() {
            val actual =
                revurdering
                    .tilVilkaarsvurdert()
                    .tilTrygdetidOppdatert()
                    .tilBeregnet()
                    .tilFattetVedtak()
                    .tilAttestert()
                    .tilTilSamordning()
                    .tilSamordnet()
                    .tilIverksatt()

            Assertions.assertEquals(BehandlingStatus.IVERKSATT, actual.status)
        }

        @Test
        fun `opprettet kan ikke gaa til andre statuser enn vilkaarsvurdert og opprettet`() {
            assertThrows<TilstandException.UgyldigTilstand> { revurdering.tilBeregnet() }
            assertThrows<TilstandException.UgyldigTilstand> { revurdering.tilFattetVedtak() }
            assertThrows<TilstandException.UgyldigTilstand> { revurdering.tilIverksatt() }
            assertThrows<TilstandException.UgyldigTilstand> { revurdering.tilReturnert() }

            assertDoesNotThrow { revurdering.tilOpprettet() }
            assertDoesNotThrow { revurdering.tilVilkaarsvurdert() }
        }

        @Test
        fun `vilkaarsvurdert kan ikke gaa til andre statuser enn trygdetid oppdatert og opprettet`() {
            val vilkaarsvurdert = revurdering.tilVilkaarsvurdert()

            assertThrows<TilstandException.UgyldigTilstand> { vilkaarsvurdert.tilBeregnet() }
            assertThrows<TilstandException.UgyldigTilstand> { vilkaarsvurdert.tilFattetVedtak() }
            assertThrows<TilstandException.UgyldigTilstand> { vilkaarsvurdert.tilIverksatt() }
            assertThrows<TilstandException.UgyldigTilstand> { vilkaarsvurdert.tilReturnert() }

            assertDoesNotThrow { vilkaarsvurdert.tilOpprettet() }
            assertDoesNotThrow { vilkaarsvurdert.tilTrygdetidOppdatert() }
        }

        @Test
        fun `trygdetid oppdatert`() {
            val trygdetidOppdatert = revurdering.tilVilkaarsvurdert().tilTrygdetidOppdatert()

            assertThrows<TilstandException.UgyldigTilstand> { trygdetidOppdatert.tilFattetVedtak() }
            assertThrows<TilstandException.UgyldigTilstand> { trygdetidOppdatert.tilIverksatt() }
            assertThrows<TilstandException.UgyldigTilstand> { trygdetidOppdatert.tilReturnert() }

            assertDoesNotThrow { trygdetidOppdatert.tilOpprettet() }
            assertDoesNotThrow { trygdetidOppdatert.tilVilkaarsvurdert() }
            assertDoesNotThrow { trygdetidOppdatert.tilTrygdetidOppdatert() }
            assertDoesNotThrow { trygdetidOppdatert.tilBeregnet() }
        }

        @Test
        fun beregnet() {
            val beregnet = revurdering.tilVilkaarsvurdert().tilTrygdetidOppdatert().tilBeregnet()

            assertThrows<TilstandException.UgyldigTilstand> { beregnet.tilReturnert() }
            assertThrows<TilstandException.UgyldigTilstand> { beregnet.tilIverksatt() }

            assertDoesNotThrow { beregnet.tilOpprettet() }
            assertDoesNotThrow { beregnet.tilVilkaarsvurdert() }
            assertDoesNotThrow { beregnet.tilTrygdetidOppdatert() }
            assertDoesNotThrow { beregnet.tilTrygdetidOppdatert() }
            assertDoesNotThrow { beregnet.tilBeregnet() }
            assertDoesNotThrow { beregnet.tilBeregnet() }
            assertDoesNotThrow { beregnet.tilFattetVedtak() }
        }
    }

    @Nested
    inner class AutomatiskStatusEndring {
        private val revurdering = opprettetRevurdering(Prosesstype.AUTOMATISK, Revurderingaarsak.REGULERING)

        @Test
        fun opprettet() {
            assertKanGaaTilAlleStatuser(revurdering)
        }

        @Test
        fun vilkaarsvurdert() {
            val vilkaarsvurdert = revurdering.tilVilkaarsvurdert()
            assertKanGaaTilAlleStatuser(vilkaarsvurdert)
        }

        @Test
        fun `trygdetid oppdatert`() {
            val beregnet = revurdering.tilVilkaarsvurdert().tilTrygdetidOppdatert()
            assertKanGaaTilAlleStatuser(beregnet)
        }

        @Test
        fun beregnet() {
            val beregnet = revurdering.tilVilkaarsvurdert().tilTrygdetidOppdatert().tilBeregnet()
            assertKanGaaTilAlleStatuser(beregnet)
        }

        @Test
        fun fattet() {
            val fattet =
                revurdering
                    .tilVilkaarsvurdert()
                    .tilTrygdetidOppdatert()
                    .tilBeregnet()
                    .tilFattetVedtak()
            assertKanGaaTilAlleStatuser(fattet)
        }

        private fun assertKanGaaTilAlleStatuser(revurdering: Behandling) {
            Assertions.assertEquals(BehandlingStatus.OPPRETTET, revurdering.tilOpprettet().status)
            Assertions.assertEquals(
                BehandlingStatus.VILKAARSVURDERT,
                revurdering.tilVilkaarsvurdert().status,
            )
            Assertions.assertEquals(
                BehandlingStatus.TRYGDETID_OPPDATERT,
                revurdering.tilTrygdetidOppdatert().status,
            )
            Assertions.assertEquals(BehandlingStatus.BEREGNET, revurdering.tilBeregnet().status)
            Assertions.assertEquals(BehandlingStatus.FATTET_VEDTAK, revurdering.tilFattetVedtak().status)
            Assertions.assertEquals(BehandlingStatus.ATTESTERT, revurdering.tilAttestert().status)
            Assertions.assertEquals(BehandlingStatus.RETURNERT, revurdering.tilReturnert().status)
            Assertions.assertEquals(BehandlingStatus.IVERKSATT, revurdering.tilIverksatt().status)
        }
    }
}

private fun opprettetRevurdering(
    prosesstype: Prosesstype,
    revurderingsaarsak: Revurderingaarsak,
): Revurdering {
    val id = UUID.randomUUID()
    return Revurdering.opprett(
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
        kommerBarnetTilgode = kommerBarnetTilGodeVurdering(id),
        virkningstidspunkt = virkningstidspunktVurdering(),
        utlandstilknytning = null,
        boddEllerArbeidetUtlandet = null,
        revurderingsaarsak = revurderingsaarsak,
        prosesstype = prosesstype,
        kilde = Vedtaksloesning.GJENNY,
        revurderingInfo = null,
        begrunnelse = null,
        relatertBehandlingId = null,
        sendeBrev = true,
    )
}
