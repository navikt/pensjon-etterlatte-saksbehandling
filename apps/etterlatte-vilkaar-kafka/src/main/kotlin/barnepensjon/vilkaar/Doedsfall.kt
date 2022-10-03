package no.nav.etterlatte.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat.IKKE_OPPFYLT
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foreldre
import java.time.LocalDateTime

fun vilkaarDoedsfallErRegistrert(
    avdoed: Grunnlagsdata<JsonNode>,
    soeker: Grunnlagsdata<JsonNode>
): VurdertVilkaar {
    val doedsdatoRegistrertIPdl = kriterieDoedsdatoRegistrertIPdl(avdoed)
    val avdoedErForeldre = kriterieAvdoedErForelder(soeker, avdoed)

    return VurdertVilkaar(
        Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
        setVilkaarVurderingFraKriterier(listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)),
        null,
        listOf(doedsdatoRegistrertIPdl, avdoedErForeldre),
        LocalDateTime.now()
    )
}

fun kriterieDoedsdatoRegistrertIPdl(avdoed: Grunnlagsdata<JsonNode>?): Kriterie =
    avdoed?.hentDoedsdato()?.let {
        val resultat = try {
            it.verdi
            VurderingsResultat.OPPFYLT
        } catch (ex: OpplysningKanIkkeHentesUt) {
            IKKE_OPPFYLT
        }
        Kriterie(
            DOEDSFALL_ER_REGISTRERT_I_PDL,
            resultat,
            listOf(
                Kriteriegrunnlag(
                    it.id,
                    KriterieOpplysningsType.DOEDSDATO,
                    it.kilde,
                    Doedsdato(it.verdi, avdoed.hentFoedselsnummer()?.verdi!!)
                )
            )
        )
    } ?: Kriterie(
        DOEDSFALL_ER_REGISTRERT_I_PDL,
        IKKE_OPPFYLT,
        emptyList()
    ) // TODO sj: Sjekke om dette faktisk blir riktig. Dersom dødsdato ikke finnes

fun kriterieAvdoedErForelder(
    soeker: Grunnlagsdata<JsonNode>,
    avdoed: Grunnlagsdata<JsonNode>
): Kriterie {
    val søkersFamilieRelasjon = soeker.hentFamilierelasjon()
    val avdødDoedsdato = avdoed.hentDoedsdato()
    val avdødFoedselsnummer = avdoed.hentFoedselsnummer()

    val opplsyningsGrunnlag = listOfNotNull(
        søkersFamilieRelasjon?.verdi?.foreldre?.let {
            Kriteriegrunnlag(
                søkersFamilieRelasjon.id,
                KriterieOpplysningsType.FORELDRE,
                søkersFamilieRelasjon.kilde,
                Foreldre(søkersFamilieRelasjon.verdi.foreldre)
            )
        },
        avdødDoedsdato?.verdi?.let {
            Kriteriegrunnlag(
                avdødDoedsdato.id,
                KriterieOpplysningsType.DOEDSDATO,
                avdødDoedsdato.kilde,
                Doedsdato(avdødDoedsdato.verdi, avdødFoedselsnummer!!.verdi) // TODO sj: ikke !!
            )
        }
    )

    val resultat =
        if (
            søkersFamilieRelasjon?.verdi?.foreldre == null ||
            avdødDoedsdato == null ||
            avdødFoedselsnummer?.verdi == null
        ) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning { søkersFamilieRelasjon.verdi.foreldre!!.contains(avdødFoedselsnummer.verdi) }
        }

    return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, resultat, opplsyningsGrunnlag)
}