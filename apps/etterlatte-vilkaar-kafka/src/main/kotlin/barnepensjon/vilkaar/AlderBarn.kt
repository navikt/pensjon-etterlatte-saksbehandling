package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import java.time.LocalDate
import java.time.LocalDateTime

fun vilkaarBrukerErUnder20(
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    virkningstidspunkt: LocalDate?
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(soekerPdl, avdoedPdl, virkningstidspunkt)

    return VurdertVilkaar(
        Vilkaartyper.SOEKER_ER_UNDER_20,
        setVilkaarVurderingFraKriterier(listOf(soekerErUnder20)),
        null,
        listOf(soekerErUnder20),
        LocalDateTime.now()
    )
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