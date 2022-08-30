package barnepensjon

import no.nav.etterlatte.barnepensjon.opplysningsGrunnlagNull
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import java.time.LocalDate
import java.time.LocalDateTime

fun vilkaarFormaalForYtelsen(soekerPdl: VilkaarOpplysning<Person>?, virkningstidspunkt: LocalDate?): VurdertVilkaar {
    val soekerErILive = kriterieSoekerErILive(soekerPdl, virkningstidspunkt)

    return VurdertVilkaar(
        Vilkaartyper.FORMAAL_FOR_YTELSEN,
        setVilkaarVurderingFraKriterier(listOf(soekerErILive)),
        null,
        listOf(soekerErILive),
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
    fun VilkaarOpplysning<Person>.doedeEtterVirk() = opplysning.doedsdato?.isAfter(virkningstidspunkt) ?: false
    fun VilkaarOpplysning<Person>.levdePaaVirkningsdato() = lever() || doedeEtterVirk()

    val resultat = vurderOpplysning { soekerPdl.levdePaaVirkningsdato() }

    return Kriterie(Kriterietyper.SOEKER_ER_I_LIVE, resultat, opplysningsGrunnlag)
}