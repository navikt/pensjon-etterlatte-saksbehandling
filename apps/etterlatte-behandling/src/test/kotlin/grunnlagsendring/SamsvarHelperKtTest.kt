package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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
                innflyttingTilNorge =
                    listOf(
                        InnflyttingTilNorge(
                            "Tyskland",
                            LocalDate.of(2013, 7, 9),
                            LocalDate.of(2013, 7, 9),
                            LocalDate.of(2013, 7, 9),
                        ),
                    ),
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
                innflyttingTilNorge =
                    listOf(
                        InnflyttingTilNorge(
                            "Tyskland",
                            LocalDate.of(2013, 7, 9),
                            LocalDate.of(2013, 7, 9),
                            LocalDate.of(2013, 7, 9),
                        ),
                    ),
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

    @Test
    fun `Samsvar adresser sjekker kun forskjell i naavaerende adresse`() {
        val naavaerendeAdresse =
            adresse(
                type = AdresseType.VEGADRESSE,
                aktiv = true,
                gyldigFraOgMed = LocalDateTime.of(2000, 1, 1, 0, 0),
                adresselinje1 = "Testveien",
            )

        val historisk1 =
            adresse(
                aktiv = false,
                gyldigFraOgMed = LocalDateTime.of(1980, 1, 1, 0, 0),
                adresselinje1 = "Gammelveien 53 c",
            )

        val historisk2 =
            adresse(
                aktiv = false,
                gyldigFraOgMed = LocalDateTime.of(1980, 1, 1, 0, 0),
                adresselinje1 = "Gammelveien 53 c)",
            )

        val samsvar =
            samsvarBostedsadresse(
                listOf(naavaerendeAdresse, historisk1),
                listOf(naavaerendeAdresse, historisk2),
            )

        assertEquals(true, samsvar.samsvar)
    }

    @Test
    fun `BOSTED-hendelse for barn tilknyttet OMS-sak skal ignoreres naar barnet er over 18 aar`() {
        val fnr = KONTANT_FOT.value
        val pdlData = mockPerson().copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(29), null))
        val hendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = fnr,
                hendelseGjelderRolle = Saksrolle.SOESKEN,
                samsvarMellomKildeOgGrunnlag = null,
            ).copy(type = GrunnlagsendringsType.BOSTED)

        val resultat =
            finnSamsvarForHendelse(
                hendelse = hendelse,
                pdlData = pdlData,
                grunnlag = null,
                personRolle = PersonRolle.TILKNYTTET_BARN,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ) as SamsvarMellomKildeOgGrunnlag.Adresse

        assertTrue(resultat.samsvar)
        assertEquals("BARN_OVER_18AAR", resultat.aarsakIgnorert)
    }

    @Test
    fun `BOSTED-hendelse for barn tilknyttet OMS-sak skal ikke ignoreres naar barnet er under 18 aar`() {
        val fnr = KONTANT_FOT.value
        val pdlAdresse =
            adresse(
                type = AdresseType.VEGADRESSE,
                aktiv = true,
                gyldigFraOgMed = LocalDateTime.of(2024, 1, 1, 0, 0),
                adresselinje1 = "Nyveien 1",
            )
        val pdlData =
            mockPerson().copy(
                foedselsdato = OpplysningDTO(LocalDate.now().minusYears(10), null),
                bostedsadresse = listOf(OpplysningDTO(pdlAdresse, null)),
            )
        val hendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = fnr,
                hendelseGjelderRolle = Saksrolle.SOESKEN,
                samsvarMellomKildeOgGrunnlag = null,
            ).copy(type = GrunnlagsendringsType.BOSTED)

        val resultat =
            finnSamsvarForHendelse(
                hendelse = hendelse,
                pdlData = pdlData,
                grunnlag = Grunnlag.empty(),
                personRolle = PersonRolle.TILKNYTTET_BARN,
                sakType = SakType.OMSTILLINGSSTOENAD,
            ) as SamsvarMellomKildeOgGrunnlag.Adresse

        assertNull(resultat.aarsakIgnorert)
        assertFalse(resultat.samsvar)
    }

    private fun adresse(
        type: AdresseType = AdresseType.VEGADRESSE,
        aktiv: Boolean = true,
        gyldigFraOgMed: LocalDateTime = LocalDateTime.now(),
        adresselinje1: String? = null,
    ): Adresse =
        Adresse(
            type = type,
            aktiv = aktiv,
            coAdresseNavn = null,
            adresseLinje1 = adresselinje1,
            adresseLinje2 = null,
            adresseLinje3 = null,
            postnr = null,
            poststed = null,
            land = null,
            kilde = "",
            gyldigFraOgMed = gyldigFraOgMed,
            gyldigTilOgMed = null,
        )
}
