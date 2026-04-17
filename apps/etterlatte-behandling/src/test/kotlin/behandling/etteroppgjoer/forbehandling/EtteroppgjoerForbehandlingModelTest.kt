package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerGrenseDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerForbehandlingModelTest {
    private val enSak = sak(sakId = sakId1)

    private fun enForbehandling() =
        EtteroppgjoerForbehandling.opprett(
            sak = enSak,
            innvilgetPeriode = Periode(YearMonth.of(2024, 1), null),
            sisteIverksatteBehandling = UUID.randomUUID(),
            mottattSkatteoppgjoer = true,
        )

    private fun beregnetResultat(type: EtteroppgjoerResultatType): BeregnetEtteroppgjoerResultatDto =
        BeregnetEtteroppgjoerResultatDto(
            id = UUID.randomUUID(),
            aar = 2024,
            forbehandlingId = UUID.randomUUID(),
            sisteIverksatteBehandlingId = UUID.randomUUID(),
            utbetaltStoenad = 1000,
            nyBruttoStoenad = 1000,
            differanse = 0,
            grense =
                EtteroppgjoerGrenseDto(
                    tilbakekreving = 0.0,
                    etterbetaling = 0.0,
                    rettsgebyr = 0,
                    rettsgebyrGyldigFra = LocalDate.of(2024, 1, 1),
                ),
            resultatType = type,
            harIngenInntekt = false,
            tidspunkt = Tidspunkt.now(),
            kilde = Grunnlagsopplysning.Saksbehandler.create("Z12345"),
            avkortingForbehandlingId = UUID.randomUUID(),
            avkortingSisteIverksatteId = UUID.randomUUID(),
            vedtakReferanse = emptyList(),
        )

    // tilBeregnet

    @Test
    fun `tilBeregnet setter status og resultatType`() {
        val resultat = enForbehandling().tilBeregnet(beregnetResultat(EtteroppgjoerResultatType.ETTERBETALING))

        resultat.status shouldBe EtteroppgjoerForbehandlingStatus.BEREGNET
        resultat.etteroppgjoerResultatType shouldBe EtteroppgjoerResultatType.ETTERBETALING
    }

    @Test
    fun `tilBeregnet fjerner brevId ved INGEN_ENDRING_UTEN_UTBETALING`() {
        val forbehandling = enForbehandling().copy(brevId = 42L)

        val resultat = forbehandling.tilBeregnet(beregnetResultat(EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING))

        resultat.brevId shouldBe null
    }

    @Test
    fun `tilBeregnet beholder brevId ved andre resultattyper`() {
        val forbehandling = enForbehandling().copy(brevId = 42L)

        val resultat = forbehandling.tilBeregnet(beregnetResultat(EtteroppgjoerResultatType.ETTERBETALING))

        resultat.brevId shouldBe 42L
    }

    @Test
    fun `tilBeregnet kaster ved ugyldig status`() {
        val forbehandling = enForbehandling().copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        shouldThrow<EtteroppgjoerForbehandlingStatusException> {
            forbehandling.tilBeregnet(beregnetResultat(EtteroppgjoerResultatType.ETTERBETALING))
        }
    }

    // tilFerdigstilt

    @Test
    fun `tilFerdigstilt fra BEREGNET med brev`() {
        val forbehandling =
            enForbehandling().copy(
                status = EtteroppgjoerForbehandlingStatus.BEREGNET,
                brevId = 1L,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        forbehandling.tilFerdigstilt().status shouldBe EtteroppgjoerForbehandlingStatus.FERDIGSTILT
    }

    @Test
    fun `tilFerdigstilt ved doedsfall i etteroppgjoersaar trenger ikke BEREGNET`() {
        val forbehandling =
            enForbehandling().copy(
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opphoerSkyldesDoedsfall = JaNei.JA,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.JA,
            )

        forbehandling.tilFerdigstilt().status shouldBe EtteroppgjoerForbehandlingStatus.FERDIGSTILT
    }

    @Test
    fun `tilFerdigstilt kaster hvis status ikke er BEREGNET og ikke doedsfall i etteroppgjoersaar`() {
        val forbehandling =
            enForbehandling().copy(
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                brevId = 1L,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        shouldThrow<EtteroppgjoerForbehandlingStatusException> {
            forbehandling.tilFerdigstilt()
        }
    }

    @Test
    fun `tilFerdigstilt kaster hvis brevId mangler og ikke kan ferdigstilles uten brev`() {
        val forbehandling =
            enForbehandling().copy(
                status = EtteroppgjoerForbehandlingStatus.BEREGNET,
                brevId = null,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        shouldThrow<InternfeilException> {
            forbehandling.tilFerdigstilt()
        }
    }

    // tilAvbrutt

    @Test
    fun `tilAvbrutt fra redigerbar forbehandling`() {
        val avbrutt = enForbehandling().tilAvbrutt(AarsakTilAvbryteForbehandling.IKKE_LENGER_AKTUELL, null)

        avbrutt.status shouldBe EtteroppgjoerForbehandlingStatus.AVBRUTT
        avbrutt.aarsakTilAvbrytelse shouldBe AarsakTilAvbryteForbehandling.IKKE_LENGER_AKTUELL
    }

    @Test
    fun `tilAvbrutt kaster hvis forbehandling er ferdigstilt`() {
        val forbehandling = enForbehandling().copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        shouldThrow<EtteroppgjoerForbehandlingStatusException> {
            forbehandling.tilAvbrutt(AarsakTilAvbryteForbehandling.ANNET, "kommentar")
        }
    }

    // kanFerdigstillesUtenBrev

    @Test
    fun `kanFerdigstillesUtenBrev er true for revurdering`() {
        val forbehandling = enForbehandling().copy(kopiertFra = UUID.randomUUID())

        forbehandling.kanFerdigstillesUtenBrev() shouldBe true
    }

    @Test
    fun `kanFerdigstillesUtenBrev er true ved INGEN_ENDRING_UTEN_UTBETALING`() {
        val forbehandling =
            enForbehandling().copy(etteroppgjoerResultatType = EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING)

        forbehandling.kanFerdigstillesUtenBrev() shouldBe true
    }

    @Test
    fun `kanFerdigstillesUtenBrev er true ved doedsfall`() {
        val forbehandling = enForbehandling().copy(opphoerSkyldesDoedsfall = JaNei.JA)

        forbehandling.kanFerdigstillesUtenBrev() shouldBe true
    }

    @Test
    fun `kanFerdigstillesUtenBrev er false ved ETTERBETALING uten doedsfall og ikke revurdering`() {
        val forbehandling =
            enForbehandling().copy(etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING)

        forbehandling.kanFerdigstillesUtenBrev() shouldBe false
    }

    // skalEtterbetalesTilDoedsbo

    @Test
    fun `skalEtterbetalesTilDoedsbo er true naar doedsfall etter etteroppgjoersaar og ETTERBETALING`() {
        val forbehandling =
            enForbehandling().copy(
                opphoerSkyldesDoedsfall = JaNei.JA,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.NEI,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        forbehandling.skalEtterbetalesTilDoedsbo() shouldBe true
    }

    @Test
    fun `skalEtterbetalesTilDoedsbo er false naar doedsfall i etteroppgjoersaar`() {
        val forbehandling =
            enForbehandling().copy(
                opphoerSkyldesDoedsfall = JaNei.JA,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.JA,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        forbehandling.skalEtterbetalesTilDoedsbo() shouldBe false
    }

    @Test
    fun `skalEtterbetalesTilDoedsbo er false naar resultat ikke er ETTERBETALING`() {
        val forbehandling =
            enForbehandling().copy(
                opphoerSkyldesDoedsfall = JaNei.JA,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.NEI,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.TILBAKEKREVING,
            )

        forbehandling.skalEtterbetalesTilDoedsbo() shouldBe false
    }

    // utledEtteroppgjoerStatus

    @Test
    fun `utledEtteroppgjoerStatus gir VENTER_PAA_SVAR ved ETTERBETALING`() {
        val forbehandling =
            enForbehandling().copy(etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING)

        forbehandling.utledEtteroppgjoerStatus() shouldBe EtteroppgjoerStatus.VENTER_PAA_SVAR
    }

    @Test
    fun `utledEtteroppgjoerStatus gir VENTER_PAA_SVAR ved TILBAKEKREVING`() {
        val forbehandling =
            enForbehandling().copy(etteroppgjoerResultatType = EtteroppgjoerResultatType.TILBAKEKREVING)

        forbehandling.utledEtteroppgjoerStatus() shouldBe EtteroppgjoerStatus.VENTER_PAA_SVAR
    }

    @Test
    fun `utledEtteroppgjoerStatus gir FERDIGSTILT ved INGEN_ENDRING_MED_UTBETALING`() {
        val forbehandling =
            enForbehandling().copy(etteroppgjoerResultatType = EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING)

        forbehandling.utledEtteroppgjoerStatus() shouldBe EtteroppgjoerStatus.FERDIGSTILT
    }

    @Test
    fun `utledEtteroppgjoerStatus gir FERDIGSTILT ved INGEN_ENDRING_UTEN_UTBETALING`() {
        val forbehandling =
            enForbehandling().copy(etteroppgjoerResultatType = EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING)

        forbehandling.utledEtteroppgjoerStatus() shouldBe EtteroppgjoerStatus.FERDIGSTILT
    }

    @Test
    fun `utledEtteroppgjoerStatus gir FERDIGSTILT ved doedsfall i etteroppgjoersaar`() {
        val forbehandling =
            enForbehandling().copy(
                opphoerSkyldesDoedsfall = JaNei.JA,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.JA,
                etteroppgjoerResultatType = EtteroppgjoerResultatType.ETTERBETALING,
            )

        forbehandling.utledEtteroppgjoerStatus() shouldBe EtteroppgjoerStatus.FERDIGSTILT
    }

    @Test
    fun `utledEtteroppgjoerStatus kaster hvis resultatType mangler`() {
        val forbehandling = enForbehandling().copy(etteroppgjoerResultatType = null)

        shouldThrow<InternfeilException> {
            forbehandling.utledEtteroppgjoerStatus()
        }
    }

    // oppdaterBrukerHarSvart

    @Test
    fun `oppdaterBrukerHarSvart setter alle felter naar mottattNyInformasjon og endringErTilUgunst er JA`() {
        val oppdatert =
            enForbehandling().oppdaterBrukerHarSvart(
                harMottattNyInformasjon = JaNei.JA,
                endringErTilUgunstForBruker = JaNei.JA,
                beskrivelseAvUgunst = "en beskrivelse",
            )

        oppdatert.harMottattNyInformasjon shouldBe JaNei.JA
        oppdatert.endringErTilUgunstForBruker shouldBe JaNei.JA
        oppdatert.beskrivelseAvUgunst shouldBe "en beskrivelse"
    }

    @Test
    fun `oppdaterBrukerHarSvart nuller ut endring og beskrivelse naar mottattNyInformasjon er NEI`() {
        val oppdatert =
            enForbehandling().oppdaterBrukerHarSvart(
                harMottattNyInformasjon = JaNei.NEI,
                endringErTilUgunstForBruker = JaNei.JA,
                beskrivelseAvUgunst = "en beskrivelse",
            )

        oppdatert.harMottattNyInformasjon shouldBe JaNei.NEI
        oppdatert.endringErTilUgunstForBruker shouldBe null
        oppdatert.beskrivelseAvUgunst shouldBe null
    }

    @Test
    fun `oppdaterBrukerHarSvart nuller ut beskrivelse naar endringErTilUgunst er NEI`() {
        val oppdatert =
            enForbehandling().oppdaterBrukerHarSvart(
                harMottattNyInformasjon = JaNei.JA,
                endringErTilUgunstForBruker = JaNei.NEI,
                beskrivelseAvUgunst = "en beskrivelse",
            )

        oppdatert.endringErTilUgunstForBruker shouldBe JaNei.NEI
        oppdatert.beskrivelseAvUgunst shouldBe null
    }

    // oppdaterOmOpphoerSkyldesDoedsfall

    @Test
    fun `oppdaterOmOpphoerSkyldesDoedsfall setter begge felter naar doedsfall er JA`() {
        val oppdatert =
            enForbehandling().oppdaterOmOpphoerSkyldesDoedsfall(
                opphoerSkyldesDoedsfall = JaNei.JA,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.JA,
            )

        oppdatert.opphoerSkyldesDoedsfall shouldBe JaNei.JA
        oppdatert.opphoerSkyldesDoedsfallIEtteroppgjoersaar shouldBe JaNei.JA
    }

    @Test
    fun `oppdaterOmOpphoerSkyldesDoedsfall nuller ut etteroppgjoersaar-felt naar doedsfall er NEI`() {
        val oppdatert =
            enForbehandling().oppdaterOmOpphoerSkyldesDoedsfall(
                opphoerSkyldesDoedsfall = JaNei.NEI,
                opphoerSkyldesDoedsfallIEtteroppgjoersaar = JaNei.JA,
            )

        oppdatert.opphoerSkyldesDoedsfall shouldBe JaNei.NEI
        oppdatert.opphoerSkyldesDoedsfallIEtteroppgjoersaar shouldBe null
    }

    // medVarselbrevSendt

    @Test
    fun `medVarselbrevSendt setter dato`() {
        val dato = LocalDate.of(2024, 3, 1)

        enForbehandling().medVarselbrevSendt(dato).varselbrevSendt shouldBe dato
    }

    @Test
    fun `medVarselbrevSendt kaster hvis dato allerede er satt`() {
        val forbehandling = enForbehandling().copy(varselbrevSendt = LocalDate.of(2024, 2, 1))

        shouldThrow<InternfeilException> {
            forbehandling.medVarselbrevSendt(LocalDate.of(2024, 3, 1))
        }
    }
}
