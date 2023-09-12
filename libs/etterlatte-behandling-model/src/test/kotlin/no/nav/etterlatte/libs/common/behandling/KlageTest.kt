package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class KlageTest {

    @Test
    fun `oppdaterFormkrav sjekker at klagen har en status som kan oppdateres`() {
        val sak = Sak(ident = "bruker", sakType = SakType.BARNEPENSJON, id = 1, enhet = "1337")
        val klage = Klage.ny(sak)

        val oppdatertKlage = assertDoesNotThrow {
            klage.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        }
        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, oppdatertKlage.status)
        Assertions.assertEquals("en saksbehandler", oppdatertKlage.formkrav?.saksbehandler?.ident)

        val klageFerdigstilt = oppdatertKlage.copy(status = KlageStatus.FERDIGSTILT)
        assertThrows<Exception> {
            klageFerdigstilt.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        }
    }

    @Test
    fun `oppdaterFormkrav setter riktig status på klagen basert på formkravene`() {
        val sak = Sak(ident = "bruker", sakType = SakType.BARNEPENSJON, id = 1, enhet = "1337")
        val klage = Klage.ny(sak)
        val klageMedFormkravOppfylt = klage.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, klageMedFormkravOppfylt.status)

        val klageMedFormkravIkkeOppfylt = klage.oppdaterFormkrav(formkravIkkeOppfylt(), "en saksbehandler")
        Assertions.assertEquals(KlageStatus.FORMKRAV_IKKE_OPPFYLT, klageMedFormkravIkkeOppfylt.status)
    }

    @Test
    fun `oppdaterFormkrav setter status tilbake fra vurdert hvis formkrav oppdateres`() {
        val sak = Sak(ident = "bruker", sakType = SakType.BARNEPENSJON, id = 1, enhet = "1337")
        val klage = Klage.ny(sak).copy(status = KlageStatus.UTFALL_VURDERT)
        val klageMedFormkravOppfylt = klage.oppdaterFormkrav(alleFormkravOppfylt(), "en saksbehandler")
        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, klageMedFormkravOppfylt.status)
    }

    private fun formkravIkkeOppfylt(): Formkrav {
        return Formkrav(
            vedtaketKlagenGjelder = null,
            erKlagerPartISaken = JaNei.NEI,
            erKlagenSignert = JaNei.NEI,
            gjelderKlagenNoeKonkretIVedtaket = JaNei.NEI,
            erKlagenFramsattInnenFrist = JaNei.NEI,
            erFormkraveneOppfylt = JaNei.NEI
        )
    }

    private fun alleFormkravOppfylt(): Formkrav {
        return Formkrav(
            vedtaketKlagenGjelder = VedtaketKlagenGjelder(
                id = "",
                behandlingId = "",
                datoAttestert = null,
                vedtakType = null
            ),
            erKlagerPartISaken = JaNei.JA,
            erKlagenSignert = JaNei.JA,
            gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
            erKlagenFramsattInnenFrist = JaNei.JA,
            erFormkraveneOppfylt = JaNei.JA
        )
    }
}