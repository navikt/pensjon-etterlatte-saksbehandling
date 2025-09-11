package no.nav.etterlatte.libs.common.behandling

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class KlageTest {
    @Test
    fun `oppdaterFormkrav sjekker at klagen har en status som kan oppdateres`() {
        val sak =
            Sak(
                ident = "bruker",
                sakType = SakType.BARNEPENSJON,
                id = sakId1,
                enhet = Enheter.defaultEnhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = null,
            )
        val klage = Klage.ny(sak, null)

        val oppdatertKlage =
            assertDoesNotThrow {
                klage.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
            }
        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, oppdatertKlage.status)
        Assertions.assertEquals("en saksbehandler", oppdatertKlage.formkrav?.saksbehandler?.ident)

        val klageAvbrutt = oppdatertKlage.avbryt(AarsakTilAvbrytelse.ANNET)
        assertThrows<Exception> {
            klageAvbrutt.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        }
    }

    @Test
    fun `oppdaterFormkrav setter riktig status på klagen basert på formkravene`() {
        val sak =
            Sak(
                ident = "bruker",
                sakType = SakType.BARNEPENSJON,
                id = sakId1,
                enhet = Enheter.defaultEnhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = null,
            )
        val klage = Klage.ny(sak, null)
        val klageMedFormkravOppfylt = klage.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, klageMedFormkravOppfylt.status)

        val klageMedFormkravIkkeOppfylt = klage.oppdaterFormkrav(formkrav(), "en saksbehandler")
        Assertions.assertEquals(KlageStatus.FORMKRAV_IKKE_OPPFYLT, klageMedFormkravIkkeOppfylt.status)
    }

    @Test
    fun `oppdaterFormkrav setter status tilbake fra vurdert hvis formkrav oppdateres`() {
        val sak =
            Sak(
                ident = "bruker",
                sakType = SakType.BARNEPENSJON,
                id = sakId1,
                enhet = Enheter.defaultEnhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = null,
            )
        val klage =
            Klage
                .ny(sak, null)
                .oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
                .oppdaterInitieltUtfallMedBegrunnelse(
                    InitieltUtfallMedBegrunnelseDto(utfall = KlageUtfall.OMGJOERING, ""),
                    "en saksbehandler",
                ).oppdaterUtfall(
                    KlageUtfallMedData.StadfesteVedtak(
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = KabalHjemmel.FTRL_1_3,
                                internKommentar = "",
                                innstillingTekst = "",
                                brev = KlageOversendelsebrev(brevId = 1L),
                            ),
                        saksbehandler = Grunnlagsopplysning.automatiskSaksbehandler,
                    ),
                )
        Assertions.assertEquals(KlageStatus.UTFALL_VURDERT, klage.status)
        val klageMedFormkravOppfylt = klage.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, klageMedFormkravOppfylt.status)
    }

    @ParameterizedTest
    @MethodSource("slettUtfallTestdata")
    fun `oppdaterFormkrav sletter eksisterende utfall hvis endring i formkravOppfylt eller klagenFramsattInnenFrist`(
        formkravOppfyltGammel: JaNei,
        formkravOppfyltNy: JaNei,
        klagenFramsattInnenFristGammel: JaNei,
        klagenFramsattInnenFristNy: JaNei,
        forventerNullstiltUtfall: Boolean,
    ) {
        val sak =
            Sak(
                ident = "bruker",
                sakType = SakType.BARNEPENSJON,
                id = sakId1,
                enhet = Enheter.defaultEnhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = null,
            )
        val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("en saksbehandler")
        val klage =
            Klage
                .ny(sak, null)
                .oppdaterFormkrav(
                    formkrav(
                        erFormkraveneOppfylt = formkravOppfyltGammel,
                        erKlagenFramsattInnenFrist = klagenFramsattInnenFristGammel,
                    ),
                    saksbehandler.ident,
                ).oppdaterInitieltUtfallMedBegrunnelse(
                    InitieltUtfallMedBegrunnelseDto(KlageUtfall.AVVIST, ""),
                    "SB",
                ).oppdaterUtfall(
                    KlageUtfallMedData.Omgjoering(
                        KlageOmgjoering(GrunnForOmgjoering.PROSESSUELL_FEIL, "svada"),
                        saksbehandler,
                    ),
                )
        val oppdatertKlage =
            klage.oppdaterFormkrav(formkrav(formkravOppfyltNy, klagenFramsattInnenFristNy), "en saksbehandler")

        if (forventerNullstiltUtfall) {
            Assertions.assertNull(oppdatertKlage.utfall)
            Assertions.assertNull(oppdatertKlage.initieltUtfall)
        } else {
            oppdatertKlage.utfall!! shouldBeEqual klage.utfall!!
            oppdatertKlage.initieltUtfall!! shouldBeEqual klage.initieltUtfall!!
        }
    }

    companion object {
        @JvmStatic
        private fun slettUtfallTestdata() =
            listOf(
                Arguments.of(JaNei.NEI, JaNei.NEI, JaNei.NEI, JaNei.NEI, false),
                Arguments.of(JaNei.NEI, JaNei.NEI, JaNei.NEI, JaNei.JA, true),
                Arguments.of(JaNei.NEI, JaNei.NEI, JaNei.JA, JaNei.NEI, true),
                Arguments.of(JaNei.NEI, JaNei.NEI, JaNei.JA, JaNei.JA, false),
                Arguments.of(JaNei.NEI, JaNei.JA, JaNei.NEI, JaNei.NEI, true),
                Arguments.of(JaNei.NEI, JaNei.JA, JaNei.NEI, JaNei.JA, true),
                Arguments.of(JaNei.NEI, JaNei.JA, JaNei.JA, JaNei.NEI, true),
                Arguments.of(JaNei.NEI, JaNei.JA, JaNei.JA, JaNei.JA, true),
                Arguments.of(JaNei.JA, JaNei.NEI, JaNei.NEI, JaNei.NEI, true),
                Arguments.of(JaNei.JA, JaNei.NEI, JaNei.NEI, JaNei.JA, true),
                Arguments.of(JaNei.JA, JaNei.NEI, JaNei.JA, JaNei.NEI, true),
                Arguments.of(JaNei.JA, JaNei.NEI, JaNei.JA, JaNei.JA, true),
                Arguments.of(JaNei.JA, JaNei.JA, JaNei.NEI, JaNei.NEI, false),
                Arguments.of(JaNei.JA, JaNei.JA, JaNei.NEI, JaNei.JA, true),
                Arguments.of(JaNei.JA, JaNei.JA, JaNei.JA, JaNei.NEI, true),
                Arguments.of(JaNei.JA, JaNei.JA, JaNei.JA, JaNei.JA, false),
            )
    }

    private fun formkrav(
        erFormkraveneOppfylt: JaNei = JaNei.NEI,
        erKlagenFramsattInnenFrist: JaNei = JaNei.NEI,
    ): Formkrav =
        Formkrav(
            vedtaketKlagenGjelder = null,
            erKlagerPartISaken = JaNei.NEI,
            erKlagenSignert = JaNei.NEI,
            gjelderKlagenNoeKonkretIVedtaket = JaNei.NEI,
            erKlagenFramsattInnenFrist = erKlagenFramsattInnenFrist,
            erFormkraveneOppfylt = erFormkraveneOppfylt,
        )

    private fun alleFormkravOppfylt(): Formkrav =
        Formkrav(
            vedtaketKlagenGjelder =
                VedtaketKlagenGjelder(
                    id = "",
                    behandlingId = "",
                    datoAttestert = null,
                    vedtakType = null,
                ),
            erKlagerPartISaken = JaNei.JA,
            erKlagenSignert = JaNei.JA,
            gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
            erKlagenFramsattInnenFrist = JaNei.JA,
            erFormkraveneOppfylt = JaNei.JA,
        )
}
