package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


fun vilkaarBrukerErUnder20(
    vilkaartype: Vilkaartyper,
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(foedselsdato, doedsdato)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(soekerErUnder20)),
        listOf(soekerErUnder20)
    )
}


fun vilkaarDoedsfallErRegistrert(
    vilkaartype: Vilkaartyper,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
    foreldre: List<VilkaarOpplysning<Foreldre>>,
): VurdertVilkaar {
    val doedsdatoRegistrertIPdl = kriterieDoedsdatoRegistrertIPdl(doedsdato)
    val avdoedErForeldre = kriterieAvdoedErForelder(foreldre, doedsdato)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)),
        listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)
    )
}

fun vilkaarAvdoedesMedlemskap(
    vilkaartype: Vilkaartyper,
    avdoedUtenlandsopphold: List<VilkaarOpplysning<Utenlandsopphold>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {

    val utenlandsoppholdSisteFemAarene = kriterieIngenUtenlandsopphold(avdoedUtenlandsopphold, doedsdato)
    // Kriterier: 1. bodd i norge siste 5 årene
    // 2. Arbeidet i norge siste 5 årene
    // 3. opphold utenfor Norge
    // ELLER :
    // 4. mottatt trydg / uføre eller pensjon siste 5 årene


    return VurdertVilkaar(
        vilkaartype,
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, //endre når vi får inn flere opplysninger
        listOf(utenlandsoppholdSisteFemAarene)
    )
}


fun kriterieIngenUtenlandsopphold(
    utenlandsopphold: List<VilkaarOpplysning<Utenlandsopphold>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {

    val oppholdUtlandetSisteFemAar = try {
        val femAarFoerDoedsdato = hentDoedsdato(doedsdato).doedsdato.minusYears(5)
        val utenlandsoppholdSoeknad = hentUtenlandsopphold(utenlandsopphold, "privatperson")
        val oppholdSisteFemAAr = utenlandsoppholdSoeknad.opphold?.map { it.tilDato?.isAfter(femAarFoerDoedsdato) }

        if (oppholdSisteFemAAr != null && oppholdSisteFemAAr.contains(true)) {
            VilkaarVurderingsResultat.IKKE_OPPFYLT
        } else {
            VilkaarVurderingsResultat.OPPFYLT
        }

    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    }

    val opplysningsGrunnlag = listOf(utenlandsopphold.filter { it.kilde.type == "privatperson" },
        doedsdato.filter { it.kilde.type == "pdl" }).flatten()
    return Kriterie(
        Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_SISTE_FEM_AAR, oppholdUtlandetSisteFemAar, opplysningsGrunnlag
    )

}

fun setVikaarVurderingsResultat(kriterie: List<Kriterie>): VilkaarVurderingsResultat {
    val resultat = kriterie.map { it.resultat }
    return if (resultat.all { it == VilkaarVurderingsResultat.OPPFYLT }) {
        VilkaarVurderingsResultat.OPPFYLT
    } else if (resultat.any { it == VilkaarVurderingsResultat.IKKE_OPPFYLT }) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    } else {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }
}

fun kriterieSoekerErUnder20(
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(foedselsdato, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat =
        vurderOpplysning { hentSoekerFoedselsdato(foedselsdato).plusYears(20) > hentVirkningsdato(doedsdato) }
    return Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, resultat, opplysningsGrunnlag)
}

fun kriterieDoedsdatoRegistrertIPdl(doedsdato: List<VilkaarOpplysning<Doedsdato>>): Kriterie {
    val resultat = try {
        hentDoedsdato(doedsdato)
        VilkaarVurderingsResultat.OPPFYLT
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    }

    return Kriterie(
        Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL,
        resultat,
        listOf(doedsdato).flatten().filter { it.kilde.type == "pdl" })
}

fun kriterieAvdoedErForelder(
    foreldre: List<VilkaarOpplysning<Foreldre>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplsyningsGrunnlag = listOf(foreldre, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat = vurderOpplysning { hentFnrForeldre(foreldre).contains(hentDoedsdato(doedsdato).foedselsnummer) }

    return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, resultat, opplsyningsGrunnlag)
}


fun vurderOpplysning(vurdering: () -> Boolean): VilkaarVurderingsResultat = try {
    if (vurdering()) VilkaarVurderingsResultat.OPPFYLT else VilkaarVurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}

fun hentFnrForeldre(foreldre: List<VilkaarOpplysning<Foreldre>>): List<String> {
    return foreldre.find { it.kilde.type == "pdl" }?.opplysning?.foreldre?.map { it.foedselsnummer.value }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentSoekerFoedselsdato(foedselsdato: List<VilkaarOpplysning<Foedselsdato>>): LocalDate {
    return foedselsdato.find { it.kilde.type == "pdl" }?.opplysning?.foedselsdato
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentDoedsdato(doedsdato: List<VilkaarOpplysning<Doedsdato>>): Doedsdato {
    return doedsdato.find { it.kilde.type == "pdl" }?.opplysning
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentVirkningsdato(doedsdato: List<VilkaarOpplysning<Doedsdato>>): LocalDate {
    val doedsdato = doedsdato.find { it.kilde.type == "pdl" }?.opplysning?.doedsdato
    return doedsdato?.with(TemporalAdjusters.firstDayOfNextMonth()) ?: throw OpplysningKanIkkeHentesUt()
}

fun hentUtenlandsopphold(
    utenlandsopphold: List<VilkaarOpplysning<Utenlandsopphold>>,
    kildetype: String
): Utenlandsopphold {
    val utenlandsopphold = utenlandsopphold.find { it.kilde.type == kildetype }?.opplysning
    return utenlandsopphold ?: throw OpplysningKanIkkeHentesUt()
}

class OpplysningKanIkkeHentesUt : IllegalStateException()




