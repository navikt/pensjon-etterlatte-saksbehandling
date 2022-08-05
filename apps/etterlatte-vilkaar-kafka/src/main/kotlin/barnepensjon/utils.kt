package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.vikaar.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OpplysningKanIkkeHentesUt constructor(override val message: String? = null) : IllegalStateException(message)

fun setVilkaarVurderingFraKriterier(kriterie: List<Kriterie>): VurderingsResultat {
    val resultat = kriterie.map { it.resultat }
    return hentVurdering(resultat)
}

fun setVilkaarVurderingFraVilkaar(vilkaar: List<VurdertVilkaar>): VurderingsResultat {
    val resultat = vilkaar
        .filter { it.navn != Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP }
        .map { it.resultat }
    return hentVurdering(resultat)
}

fun setVurderingFraKommerBarnetTilGode(vilkaar: List<VurdertVilkaar>): VurderingsResultat {
    val gjenlevendeBarnSammeAdresse =
        vilkaar.find { it.navn == Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE }?.resultat
    val barnIngenUtlandsadresse =
        vilkaar.find { it.navn == Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE }?.resultat
    val avdoedAdresse =
        vilkaar.find { it.navn == Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE }?.resultat
    val saksbehandlerResultat =
        vilkaar.find { it.navn == Vilkaartyper.SAKSBEHANDLER_RESULTAT }?.resultat

    return if (saksbehandlerResultat != null) {
        saksbehandlerResultat
    } else if (gjenlevendeBarnSammeAdresse != VurderingsResultat.IKKE_OPPFYLT) {
        hentVurdering(listOf(gjenlevendeBarnSammeAdresse, barnIngenUtlandsadresse))
    } else {
        hentVurdering(listOf(gjenlevendeBarnSammeAdresse, barnIngenUtlandsadresse, avdoedAdresse))
    }
}

fun hentVurdering(resultat: List<VurderingsResultat?>): VurderingsResultat {
    return if (resultat.all { it == VurderingsResultat.OPPFYLT }) {
        VurderingsResultat.OPPFYLT
    } else if (resultat.any { it == VurderingsResultat.IKKE_OPPFYLT }) {
        VurderingsResultat.IKKE_OPPFYLT
    } else {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }
}

fun hentSisteVurderteDato(vilkaar: List<VurdertVilkaar>): LocalDateTime {
    val datoer = vilkaar.map { it.vurdertDato }
    return datoer.maxOf { it }
}

fun vurderOpplysning(vurdering: () -> Boolean): VurderingsResultat = try {
    if (vurdering()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}

fun opplysningsGrunnlagNull(
    kriterietype: Kriterietyper,
    opplysningsGrunnlag: List<Kriteriegrunnlag<out Any>>
): Kriterie {
    return Kriterie(
        kriterietype,
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
        opplysningsGrunnlag
    )
}

fun harKunNorskePdlAdresserEtterDato(
    adresserPdl: Adresser,
    dato: LocalDate,
): VurderingsResultat {
    val bostedResultat = harKunNorskeAdresserEtterDato(adresserPdl.bostedadresse, dato)
    val oppholdResultat = harKunNorskeAdresserEtterDato(adresserPdl.oppholdadresse, dato)
    val kontaktResultat = harKunNorskeAdresserEtterDato(adresserPdl.kontaktadresse, dato)

    return if (listOf(
            bostedResultat,
            oppholdResultat,
            kontaktResultat
        ).contains(VurderingsResultat.IKKE_OPPFYLT)
    ) {
        VurderingsResultat.IKKE_OPPFYLT
    } else {
        VurderingsResultat.OPPFYLT
    }
}

fun harKunNorskeAdresserEtterDato(adresser: List<Adresse>?, dato: LocalDate): VurderingsResultat {
    val adresserEtterDato =
        adresser?.filter { it.gyldigTilOgMed?.toLocalDate()?.isAfter(dato) == true || it.aktiv }
    val harUtenlandskeAdresserIPdl =
        adresserEtterDato?.any { it.type == AdresseType.UTENLANDSKADRESSE || it.type == AdresseType.UTENLANDSKADRESSEFRITTFORMAT }

    return if (harUtenlandskeAdresserIPdl == true) {
        VurderingsResultat.IKKE_OPPFYLT
    } else {
        VurderingsResultat.OPPFYLT
    }
}

fun hentAdresseperioderINorge(adresser: List<Adresse>?, doedsdato: LocalDate): List<Periode>? {
    val femAarFoerDoedsdato = doedsdato.minusYears(5)
    val adresserEtterDato =
        adresser?.filter { it.gyldigTilOgMed?.toLocalDate()?.isAfter(femAarFoerDoedsdato) == true || it.aktiv }
    val norskeAdresserEtterDato =
        adresserEtterDato?.filter { it.type != AdresseType.UTENLANDSKADRESSE && it.type != AdresseType.UTENLANDSKADRESSEFRITTFORMAT }

    val adresseperioder = norskeAdresserEtterDato?.map { setPerioder(it, doedsdato, femAarFoerDoedsdato) }

    return adresseperioder?.filterNotNull()?.sortedBy { it.gyldigFra }
}

data class Periode(
    var gyldigFra: LocalDate,
    var gyldigTil: LocalDate
)

fun setPerioder(adresse: Adresse, doedsdato: LocalDate, femAarFoerDoedsdato: LocalDate): Periode? {
    fun hentGyldigFra(): LocalDate? {
        if (adresse.gyldigFraOgMed?.toLocalDate()?.isBefore(femAarFoerDoedsdato) == true) {
            return femAarFoerDoedsdato
        } else {
            return adresse.gyldigFraOgMed?.toLocalDate()
        }
    }

    fun hentGyldigTil(): LocalDate? {
        if (adresse.gyldigTilOgMed == null && adresse.aktiv == true) {
            return doedsdato
        } else if (adresse.gyldigTilOgMed?.toLocalDate()?.isAfter(doedsdato) == true) {
            return doedsdato
        } else {

            return adresse.gyldigTilOgMed?.toLocalDate()
        }
    }

    val fra = hentGyldigFra()
    val til = hentGyldigTil()

    return if (fra != null && til != null) {
        Periode(fra, til)
    } else {
        null
    }

}

fun kombinerPerioder(perioder: List<Periode>?): Stack<Periode>? {
    val stack: Stack<Periode> = Stack()
    if (!perioder.isNullOrEmpty()) {
        stack.push(perioder[0])

        for (periode in perioder) {
            val top: Periode = stack.peek()

            if (top.gyldigTil.isBefore(periode.gyldigFra)) {
                stack.push(periode)
            } else if (top.gyldigTil.isBefore(periode.gyldigTil)) {
                top.gyldigTil = periode.gyldigTil
                stack.pop()
                stack.push(top)
            }
        }
        return stack
    } else {
        return null
    }
}

fun hentGaps(stack: Stack<Periode>?, femAarFoerDoedsdato: LocalDate, doedsdato: LocalDate): List<Periode> {
    var gaps: List<Periode> = emptyList()

    if (stack.isNullOrEmpty()) {
        val gap = Periode(femAarFoerDoedsdato, doedsdato)
        gaps = gaps.plus(gap)
    } else {
        val liste = stack.toList().sortedBy { it.gyldigFra }
        val listeDes = liste.sortedByDescending { it.gyldigTil }


        if (liste.first().gyldigFra.isAfter(femAarFoerDoedsdato)) {
            val gap = Periode(femAarFoerDoedsdato, liste[0].gyldigFra.minusDays(1))
            gaps = gaps.plus(gap)
        }

        if (listeDes.first().gyldigTil.isBefore(doedsdato)) {
            val gap = Periode(listeDes.first().gyldigTil.plusDays(1), doedsdato)
            gaps = gaps.plus(gap)
        }

        if (liste.size > 1) {
            for (i in 0 until (liste.size - 1)) {
                if (liste[i].gyldigTil.isBefore(liste[i + 1].gyldigFra)) {
                    val gap = Periode(liste[i].gyldigTil.plusDays(1), liste[i + 1].gyldigFra.minusDays(1))
                    gaps = gaps.plus(gap)
                }
            }
        }
    }
    return gaps.sortedBy { it.gyldigFra }
}
