package barnepensjon

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.PersonInfo
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.vilkaar.barnepensjon.brukerErUnder20
import no.nav.etterlatte.vilkaar.barnepensjon.doedsfallErRegistrert
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*

internal class BarnepensjonVilkaarTest {

    @Test
    fun vurderAlderErUnder20() {
        val vurderingBarnOver20 = brukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20.value, listOf(foedselsdatoBarnOver20), listOf(doedsdatoForelderPdl))
        val vurderingBarnUnder20 = brukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20.value, listOf(foedselsdatoBarnUnder20), listOf(doedsdatoForelderPdl))
        val vurderingBarnUnder20UtenDoedsdato = brukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20.value, listOf(foedselsdatoBarnUnder20), listOf(doedsdatoForelderSoeknad))

        assertEquals(vurderingBarnOver20.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(vurderingBarnUnder20.resultat, VilkaarVurderingsResultat.OPPFYLT)
        assertEquals(vurderingBarnUnder20UtenDoedsdato.resultat, VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)

    }

    @Test
    fun vurderDoedsdatoErRegistrert() {
        val doedsdatoIkkeIPdl =
            doedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT.value, listOf(doedsdatoForelderSoeknad), listOf(foreldre))

        val avdoedErForelder =
            doedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT.value, listOf(doedsdatoForelderPdl), listOf(foreldre))

        val avdoedIkkeForelder =
            doedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT.value, listOf(doedsdatoIkkeForelderPdl), listOf(foreldre))

        assertEquals(doedsdatoIkkeIPdl.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(avdoedErForelder.resultat, VilkaarVurderingsResultat.OPPFYLT)
        assertEquals(avdoedIkkeForelder.resultat, VilkaarVurderingsResultat.IKKE_OPPFYLT)
        assertEquals(avdoedErForelder.basertPaaOpplysninger[0].opplysning, doedsdatoForelderPdl.opplysning)

    }

    val foedselsdatoBarnOver20 = VilkaarOpplysning(
        opplysingType = Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Foedselsdato(LocalDate.parse("2000-08-29"), "29082076127")
    )

    val foedselsdatoBarnUnder20 = VilkaarOpplysning(
        opplysingType = Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Foedselsdato(LocalDate.parse("2020-06-10"), "06102076127")
    )

    val doedsdatoForelderPdl = VilkaarOpplysning(
        opplysingType = Opplysningstyper.AVDOED_DOEDSFALL_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Doedsdato(LocalDate.parse("2022-01-25"), "19078504903")
    )

    val doedsdatoIkkeForelderPdl = VilkaarOpplysning(
        opplysingType = Opplysningstyper.AVDOED_DOEDSFALL_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Doedsdato(LocalDate.parse("2022-01-25"), "11057523044")
    )

    val doedsdatoForelderSoeknad = VilkaarOpplysning(
        opplysingType = Opplysningstyper.AVDOED_DOEDSFALL_V1.value,
        kilde = Behandlingsopplysning.Privatperson("19078504903", Instant.now()),
        opplysning = Doedsdato(LocalDate.parse("2022-01-25"), "19078504903")
    )

    val foreldre = VilkaarOpplysning(
        opplysingType = Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1.value,
        kilde = Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysning = Foreldre(
            listOf(
                PersonInfo(
                    "Test",
                    "Testulfsen",
                    Foedselsnummer.of("19078504903"),
                    PersonType.FORELDER
                )
            )
        )
    )

}