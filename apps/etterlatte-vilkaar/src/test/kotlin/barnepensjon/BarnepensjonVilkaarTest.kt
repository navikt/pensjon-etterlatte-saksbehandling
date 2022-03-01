package barnepensjon

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.PersonInfo
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.vilkaar.barnepensjon.hentDoedsdato
import no.nav.etterlatte.vilkaar.barnepensjon.kriterieIngenUtenlandsopphold
import no.nav.etterlatte.vilkaar.barnepensjon.setVikaarVurderingsResultat
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarDoedsfallErRegistrert
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*

internal class BarnepensjonVilkaarTest {

    @Test
    fun vurderAlderErUnder20() {
        val vurderingBarnOver20 = vilkaarBrukerErUnder20(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            listOf(foedselsdatoBarnOver20),
            listOf(doedsdatoForelderPdl)
        )
        val vurderingBarnUnder20 = vilkaarBrukerErUnder20(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            listOf(foedselsdatoBarnUnder20),
            listOf(doedsdatoForelderPdl)
        )
        val vurderingBarnUnder20UtenDoedsdato = vilkaarBrukerErUnder20(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            listOf(foedselsdatoBarnUnder20),
            listOf(doedsdatoForelderSoeknad)
        )

        assertEquals(vurderingBarnOver20.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(vurderingBarnUnder20.resultat, VilkaarVurderingsResultat.OPPFYLT)
        assertEquals(
            vurderingBarnUnder20UtenDoedsdato.resultat,
            VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        )

    }

    @Test
    fun vurderDoedsdatoErRegistrert() {
        val doedsdatoIkkeIPdl =
            vilkaarDoedsfallErRegistrert(
                Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
                listOf(doedsdatoForelderSoeknad),
                listOf(foreldre)
            )

        val avdoedErForelder =
            vilkaarDoedsfallErRegistrert(
                Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
                listOf(doedsdatoForelderPdl),
                listOf(foreldre)
            )

        val avdoedIkkeForelder =
            vilkaarDoedsfallErRegistrert(
                Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
                listOf(doedsdatoIkkeForelderPdl),
                listOf(foreldre)
            )

        assertEquals(doedsdatoIkkeIPdl.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(avdoedErForelder.resultat, VilkaarVurderingsResultat.OPPFYLT)
        assertEquals(avdoedIkkeForelder.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)

    }

    @Test
    fun vurderAvdoedesForutgaaendeMeldemskap() {
        val utenlandsopphold = kriterieIngenUtenlandsopphold(listOf(utenlandsopphold), listOf(doedsdatoForelderPdl))
        val ingenUtenlandsopphold = kriterieIngenUtenlandsopphold(listOf(ingenUtenlandsopphold), listOf(doedsdatoForelderPdl))

        assertEquals(utenlandsopphold.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(ingenUtenlandsopphold.resultat, VilkaarVurderingsResultat.OPPFYLT)

    }

    @Test
    fun vurderVilkaarsVurdering() {

        val kriterieOppfylt =
            Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, VilkaarVurderingsResultat.OPPFYLT, listOf())
        val kriterieIkkeOppfylt =
            Kriterie(
                Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
                VilkaarVurderingsResultat.IKKE_OPPFYLT,
                listOf()
            )
        val kriterieKanIkkeVurdere = Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            listOf()
        )

        val vilkaarKriterierOppfylt = setVikaarVurderingsResultat(listOf(kriterieOppfylt, kriterieOppfylt))
        val vilkaarEtKriterieIkkeOppfylt =
            setVikaarVurderingsResultat(listOf(kriterieOppfylt, kriterieIkkeOppfylt, kriterieKanIkkeVurdere))
        val vilkaarKriterierOppfyltOgKanIkkeHentesUt =
            setVikaarVurderingsResultat(listOf(kriterieOppfylt, kriterieKanIkkeVurdere, kriterieOppfylt))

        assertEquals(VilkaarVurderingsResultat.OPPFYLT, vilkaarKriterierOppfylt)
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, vilkaarEtKriterieIkkeOppfylt)
        assertEquals(
            VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vilkaarKriterierOppfyltOgKanIkkeHentesUt
        )

    }


    val foedselsdatoBarnOver20 = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Foedselsdato(LocalDate.parse("2000-08-29"), "29082076127")
    )

    val foedselsdatoBarnUnder20 = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Foedselsdato(LocalDate.parse("2020-06-10"), "06102076127")
    )

    val doedsdatoForelderPdl = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.AVDOED_DOEDSFALL_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Doedsdato(LocalDate.parse("2022-01-25"), "19078504903")
    )

    val doedsdatoIkkeForelderPdl = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.AVDOED_DOEDSFALL_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Doedsdato(LocalDate.parse("2022-01-25"), "11057523044")
    )

    val doedsdatoForelderSoeknad = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.AVDOED_DOEDSFALL_V1.value,
        kilde = Behandlingsopplysning.Privatperson("19078504903", Instant.now()),
        opplysning = Doedsdato(LocalDate.parse("2022-01-25"), "19078504903")
    )

    val foreldre = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Foreldre(
            listOf(
                PersonInfo(
                    "Test",
                    "Testulfsen",
                    Foedselsnummer.of("19078504903"),
                    "Adresse",
                    PersonType.FORELDER
                )
            )
        )
    )

    val utenlandsopphold = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.AVDOED_UTENLANDSOPPHOLD_V1.value,
        kilde = Behandlingsopplysning.Privatperson("19078504903", Instant.now()),
        opplysning = Utenlandsopphold(
            "JA",
            listOf(
                UtenlandsoppholdOpplysninger(
                    "Danmark",
                    LocalDate.parse("2010-01-25"),
                    LocalDate.parse("2022-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    "JA",
                    null
                ),
                UtenlandsoppholdOpplysninger(
                    "Costa Rica",
                    LocalDate.parse("2000-01-25"),
                    LocalDate.parse("2007-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    "NEI",
                    null
                ),

                ),
            "19078504903"
        )
    )

    val ingenUtenlandsopphold = VilkaarOpplysning(
        opplysningsType = Opplysningstyper.AVDOED_UTENLANDSOPPHOLD_V1.value,
        kilde = Behandlingsopplysning.Privatperson("19078504903", Instant.now()),
        opplysning = Utenlandsopphold(
            "NEI",
            listOf(),
            "19078504903"
        )

    )

}