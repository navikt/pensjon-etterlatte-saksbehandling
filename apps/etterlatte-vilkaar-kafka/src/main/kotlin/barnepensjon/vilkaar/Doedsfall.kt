package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foreldre
import java.time.LocalDateTime


fun vilkaarDoedsfallErRegistrert(
    vilkaartype: Vilkaartyper,
    avdoed: VilkaarOpplysning<Person>?,
    soeker: VilkaarOpplysning<Person>?,
): VurdertVilkaar {
    val doedsdatoRegistrertIPdl = kriterieDoedsdatoRegistrertIPdl(avdoed)
    val avdoedErForeldre = kriterieAvdoedErForelder(soeker, avdoed)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingFraKriterier(listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)),
        listOf(doedsdatoRegistrertIPdl, avdoedErForeldre),
        LocalDateTime.now()
    )
}

fun kriterieDoedsdatoRegistrertIPdl(avdoed: VilkaarOpplysning<Person>?): Kriterie {
    return avdoed?.let {
        val resultat = try {
            hentDoedsdato(avdoed)
            VurderingsResultat.OPPFYLT
        } catch (ex: OpplysningKanIkkeHentesUt) {
            VurderingsResultat.IKKE_OPPFYLT
        }
        Kriterie(
            Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL,
            resultat,
            listOf(
                Kriteriegrunnlag(
                    avdoed.id,
                    KriterieOpplysningsType.DOEDSDATO,
                    avdoed.kilde,
                    Doedsdato(avdoed.opplysning.doedsdato, avdoed.opplysning.foedselsnummer)
                )
            )
        )
    } ?: opplysningsGrunnlagNull(Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL, emptyList())
}

fun kriterieAvdoedErForelder(
    soeker: VilkaarOpplysning<Person>?,
    avdoed: VilkaarOpplysning<Person>?,
): Kriterie {
    val opplsyningsGrunnlag = listOfNotNull(
        soeker?.let {
            Kriteriegrunnlag(
                soeker.id,
                KriterieOpplysningsType.FORELDRE,
                soeker.kilde,
                Foreldre(soeker.opplysning.familieRelasjon?.foreldre)
            )
        },
        avdoed?.let {
            Kriteriegrunnlag(
                avdoed.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdoed.kilde,
                Doedsdato(avdoed.opplysning.doedsdato, avdoed.opplysning.foedselsnummer)
            )
        }
    )

    val resultat = if (soeker == null || avdoed == null) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else {
        vurderOpplysning { hentFnrForeldre(soeker).contains(avdoed.opplysning.foedselsnummer) }
    }

    return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, resultat, opplsyningsGrunnlag)
}




