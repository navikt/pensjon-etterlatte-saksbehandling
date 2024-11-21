package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SamsvarHelperKtTest {
    @Test
    fun `samsvarDoedsdatoer med samsvar`() {
        val doedsdatoPdl = LocalDate.now()
        val doedsdatoGrunnlag = LocalDate.now()
        val resultat = samsvarDoedsdatoer(doedsdatoPdl, doedsdatoGrunnlag)
        assertTrue(resultat.samsvar)
    }

    @Test
    fun `samsvarDoedsdatoer uten samsvar`() {
        val doedsdatoPdl = LocalDate.now()
        val doedsdatoGrunnlag = null
        val resultat = samsvarDoedsdatoer(doedsdatoPdl, doedsdatoGrunnlag)
        assertFalse(resultat.samsvar)
    }

    @Test
    fun `samsvarAnsvarligeForeldre med samsvar`() {
        val ansvarligeForeldrePdl = listOf(JOVIAL_LAMA, KONTANT_FOT)
        val ansvarligeForeldreGrunnlag = listOf(JOVIAL_LAMA, KONTANT_FOT)
        val resultat = samsvarAnsvarligeForeldre(ansvarligeForeldrePdl, ansvarligeForeldreGrunnlag)
        assertTrue(resultat.samsvar)
    }

    @Test
    fun `samsvarAnsvarligeForeldre uten samsvar`() {
        val ansvarligeForeldrePdl = listOf(JOVIAL_LAMA, KONTANT_FOT)
        val ansvarligeForeldreGrunnlag = listOf(JOVIAL_LAMA)
        val resultat = samsvarAnsvarligeForeldre(ansvarligeForeldrePdl, ansvarligeForeldreGrunnlag)
        assertFalse(resultat.samsvar)
    }

    @Test
    fun `samsvarBarn med samsvar`() {
        val barnPdl = listOf(JOVIAL_LAMA, KONTANT_FOT)
        val barnGrunnlag = listOf(KONTANT_FOT, JOVIAL_LAMA)
        val resultat = samsvarBarn(barnPdl, barnGrunnlag)
        assertTrue(resultat.samsvar)
    }

    @Test
    fun `samsvarBarn uten samsvar`() {
        val barnPdl = listOf(JOVIAL_LAMA, KONTANT_FOT)
        val barnGrunnlag = listOf(KONTANT_FOT)
        val resultat = samsvarBarn(barnPdl, barnGrunnlag)
        assertFalse(resultat.samsvar)
    }

    @Test
    fun `samsvarUtflytting med samsvar`() {
        val utland =
            Utland(
                innflyttingTilNorge = listOf(InnflyttingTilNorge("Tyskland", LocalDate.of(2013, 7, 9))),
                utflyttingFraNorge = listOf(UtflyttingFraNorge("Tyskland", LocalDate.of(2022, 1, 1))),
            )
        val utflyttingPdl = utland
        val utflyttingGrunnlag = utland
        val resultat = samsvarUtflytting(utflyttingPdl, utflyttingGrunnlag)
        assertTrue(resultat.samsvar)
    }

    @Test
    fun `samsvarUtflytting uten samsvar`() {
        val utflyttingPdl =
            Utland(
                innflyttingTilNorge = listOf(InnflyttingTilNorge("Tyskland", LocalDate.of(2013, 7, 9))),
                utflyttingFraNorge = listOf(UtflyttingFraNorge("Tyskland", LocalDate.of(2022, 1, 1))),
            )
        val utflyttingGrunnlag =
            Utland(
                innflyttingTilNorge = null,
                utflyttingFraNorge = listOf(UtflyttingFraNorge("Tyskland", LocalDate.of(2022, 1, 1))),
            )
        val resultat = samsvarUtflytting(utflyttingPdl, utflyttingGrunnlag)
        assertFalse(resultat.samsvar)
    }

    @Test
    fun `samsvarSivilstand med samsvar`() {
        val sivilstand =
            listOf(
                Sivilstand(
                    sivilstatus = Sivilstatus.GIFT,
                    relatertVedSiviltilstand = null,
                    gyldigFraOgMed = LocalDate.now(),
                    bekreftelsesdato = LocalDate.now(),
                    kilde = "test",
                ),
            )

        val resultat = samsvarSivilstandOMS(sivilstand, sivilstand)
        assertTrue(resultat.samsvar)
    }

    @Test
    fun `samsvarSivilstand uten samsvar`() {
        val sivilstand1 =
            listOf(
                Sivilstand(
                    sivilstatus = Sivilstatus.GIFT,
                    relatertVedSiviltilstand = null,
                    gyldigFraOgMed = LocalDate.now(),
                    bekreftelsesdato = LocalDate.now(),
                    kilde = "test",
                ),
            )

        val sivilstand2 =
            listOf(
                Sivilstand(
                    sivilstatus = Sivilstatus.UGIFT,
                    relatertVedSiviltilstand = null,
                    gyldigFraOgMed = LocalDate.now(),
                    bekreftelsesdato = LocalDate.now(),
                    kilde = "test",
                ),
            )

        val resultat = samsvarSivilstandOMS(sivilstand1, sivilstand2)
        assertFalse(resultat.samsvar)
    }

    @Test
    fun `Sjekk samsvar folkeregisterident - endret ident`() {
        val pdlIdent = KONTANT_FOT
        val grunnlagIdent = JOVIAL_LAMA

        val resultat = samsvarFolkeregisterIdent(pdlIdent, grunnlagIdent)
        assertFalse(resultat.samsvar)
        assertEquals(pdlIdent, resultat.fraPdl)
        assertEquals(grunnlagIdent, resultat.fraGrunnlag)
    }

    @Test
    fun `Sjekk samsvar folkeregisterident - ingen endring`() {
        val pdlIdent = KONTANT_FOT
        val grunnlagIdent = KONTANT_FOT

        val resultat = samsvarFolkeregisterIdent(pdlIdent, grunnlagIdent)
        assertTrue(resultat.samsvar)
        assertEquals(pdlIdent, resultat.fraPdl)
        assertEquals(grunnlagIdent, resultat.fraGrunnlag)
    }
}
