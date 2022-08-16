package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.*
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import java.time.LocalDate
import java.time.LocalDateTime


fun vilkaarBrukerErUnder20(
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    virkningstidspunkt: LocalDate?,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(soekerPdl, avdoedPdl, virkningstidspunkt)
    val soekerErILive = kriterieSoekerErILive(soekerPdl, virkningstidspunkt)

    return VurdertVilkaar(
        Vilkaartyper.SOEKER_ER_UNDER_20,
        setVilkaarVurderingFraKriterier(listOf(soekerErUnder20, soekerErILive)),
        null,
        listOf(soekerErUnder20, soekerErILive),
        LocalDateTime.now()
    )
}

fun kriterieSoekerErILive(soekerPdl: VilkaarOpplysning<Person>?, virkningstidspunkt: LocalDate?): Kriterie {
    if (soekerPdl == null || virkningstidspunkt == null) {
        return opplysningsGrunnlagNull(Kriterietyper.SOEKER_ER_I_LIVE, emptyList())
    }

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            soekerPdl.id,
            KriterieOpplysningsType.DOEDSDATO,
            soekerPdl.kilde,
            Doedsdato(soekerPdl.opplysning.doedsdato, soekerPdl.opplysning.foedselsnummer)
        )
    )

    fun VilkaarOpplysning<Person>.lever() = opplysning.doedsdato == null
    fun VilkaarOpplysning<Person>.doedeEtterVirk() = opplysning.doedsdato?.isAfter(virkningstidspunkt)?: false
    fun VilkaarOpplysning<Person>.levdePaaVirkningsdato() = lever() || doedeEtterVirk()

    val resultat = vurderOpplysning { soekerPdl.levdePaaVirkningsdato() }

    return Kriterie(Kriterietyper.SOEKER_ER_I_LIVE, resultat, opplysningsGrunnlag)
}

fun kriterieSoekerErUnder20(
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    virkningstidspunkt: LocalDate?
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        soekerPdl?.let {
            Kriteriegrunnlag(
                soekerPdl.id,
                KriterieOpplysningsType.FOEDSELSDATO,
                soekerPdl.kilde,
                Foedselsdato(soekerPdl.opplysning.foedselsdato, soekerPdl.opplysning.foedselsnummer)
            )
        },
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        }
    )

    val resultat = if (soekerPdl == null || virkningstidspunkt == null) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else {
        vurderOpplysning { hentFoedselsdato(soekerPdl).plusYears(18) > virkningstidspunkt }
    }

    return Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            resultat,
            opplysningsGrunnlag
        )
}

