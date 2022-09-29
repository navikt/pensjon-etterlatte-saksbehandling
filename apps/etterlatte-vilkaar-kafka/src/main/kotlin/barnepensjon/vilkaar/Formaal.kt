package barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDate
import java.time.LocalDateTime

fun vilkaarFormaalForYtelsen(søker: Grunnlagsdata<JsonNode>?, virkningstidspunkt: LocalDate?): VurdertVilkaar {
    val soekerErILive = kriterieSoekerErILive(søker, virkningstidspunkt)

    return VurdertVilkaar(
        Vilkaartyper.FORMAAL_FOR_YTELSEN,
        setVilkaarVurderingFraKriterier(listOf(soekerErILive)),
        null,
        listOf(soekerErILive),
        LocalDateTime.now()
    )
}

fun kriterieSoekerErILive(søker: Grunnlagsdata<JsonNode>?, virkningstidspunkt: LocalDate?): Kriterie {
    val dødsdato = søker?.hentDoedsdato()
    if (søker == null || virkningstidspunkt == null) {
        return opplysningsGrunnlagNull(Kriterietyper.SOEKER_ER_I_LIVE, emptyList())
    }

    if (dødsdato == null) {
        return Kriterie(Kriterietyper.SOEKER_ER_I_LIVE, VurderingsResultat.OPPFYLT, emptyList())
    }

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            dødsdato.id,
            KriterieOpplysningsType.DOEDSDATO,
            dødsdato.kilde,
            Doedsdato(dødsdato.verdi, søker.hentFoedselsnummer()?.verdi!!)
        )
    )

    val levdePåVirkningsdato = dødsdato.verdi!!.isAfter(virkningstidspunkt)
    val resultat = vurderOpplysning { levdePåVirkningsdato }

    return Kriterie(Kriterietyper.SOEKER_ER_I_LIVE, resultat, opplysningsGrunnlag)
}