package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.*
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import java.time.LocalDateTime


fun vilkaarBrukerErUnder20(
    vilkaartype: Vilkaartyper,
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(soekerPdl, avdoedPdl)
    val soekerErILive = kriterieSoekerErILive(soekerPdl, avdoedPdl)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingFraKriterier(listOf(soekerErILive, soekerErUnder20)),
        null,
        listOf(soekerErILive, soekerErUnder20),
        LocalDateTime.now()
    )
}

fun kriterieSoekerErILive(soekerPdl: VilkaarOpplysning<Person>?, avdoedPdl: VilkaarOpplysning<Person>?): Kriterie {
    if (soekerPdl == null || avdoedPdl == null) {
        return Kriterie(
            Kriterietyper.SOEKER_ER_I_LIVE,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            emptyList()
        )
    }

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            soekerPdl.id,
            KriterieOpplysningsType.DOEDSDATO,
            soekerPdl.kilde,
            Doedsdato(soekerPdl.opplysning.doedsdato, soekerPdl.opplysning.foedselsnummer)
        ),
        Kriteriegrunnlag(
            avdoedPdl.id,
            KriterieOpplysningsType.DOEDSDATO,
            avdoedPdl.kilde,
            Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
        )
    )
    // TODO ai: Sjekk om det er vits att søker med dødsdato fortsatt har oppfyllt vurdering
    val resultat = vurderOpplysning { soekerPdl.opplysning.doedsdato == null || (soekerPdl.opplysning.doedsdato!!.isAfter(hentVirkningsdato(avdoedPdl))) }

    return Kriterie(Kriterietyper.SOEKER_ER_I_LIVE, resultat, opplysningsGrunnlag)
}

fun kriterieSoekerErUnder20(
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        },
        soekerPdl?.let {
            Kriteriegrunnlag(
                soekerPdl.id,
                KriterieOpplysningsType.FOEDSELSDATO,
                soekerPdl.kilde,
                Foedselsdato(soekerPdl.opplysning.foedselsdato, soekerPdl.opplysning.foedselsnummer)
            )
        },
    )

    val resultat = if (soekerPdl == null || avdoedPdl == null) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else {
        vurderOpplysning { hentFoedselsdato(soekerPdl).plusYears(18) > hentVirkningsdato(avdoedPdl) }
    }

    return Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            resultat,
            opplysningsGrunnlag
        )
}

