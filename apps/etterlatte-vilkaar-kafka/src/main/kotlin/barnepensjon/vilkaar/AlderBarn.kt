package no.nav.etterlatte.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import java.time.LocalDate
import java.time.LocalDateTime

fun vilkaarBrukerErUnder20(
    søker: Grunnlagsdata<JsonNode>?,
    avdød: Grunnlagsdata<JsonNode>?,
    virkningstidspunkt: LocalDate?
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(søker, avdød, virkningstidspunkt)

    return VurdertVilkaar(
        Vilkaartyper.SOEKER_ER_UNDER_20,
        setVilkaarVurderingFraKriterier(listOf(soekerErUnder20)),
        null,
        listOf(soekerErUnder20),
        LocalDateTime.now()
    )
}
fun kriterieSoekerErUnder20(
    søker: Grunnlagsdata<JsonNode>?,
    avdød: Grunnlagsdata<JsonNode>?,
    virkningstidspunkt: LocalDate?
): Kriterie {
    val søkerFødselsdato = søker?.hentFoedselsdato()
    val søkerFødselsnummer = søker?.hentFoedselsnummer()
    val avdødDødsdato = avdød?.hentDoedsdato()
    val avdødFødselsnummer = avdød?.hentFoedselsnummer()

    if (
        søkerFødselsdato == null ||
        søkerFødselsnummer == null ||
        virkningstidspunkt == null ||
        avdødDødsdato == null ||
        avdødFødselsnummer == null
    ) {
        return Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            emptyList()
        )
    }

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            søkerFødselsdato.id,
            KriterieOpplysningsType.FOEDSELSDATO,
            søkerFødselsdato.kilde,
            Foedselsdato(søkerFødselsdato.verdi, søkerFødselsnummer.verdi)
        ),
        Kriteriegrunnlag(
            avdødDødsdato.id,
            KriterieOpplysningsType.DOEDSDATO,
            avdødDødsdato.kilde,
            Doedsdato(avdødDødsdato.verdi, avdødFødselsnummer.verdi)
        )
    )

    return Kriterie(
        Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
        vurderOpplysning { søkerFødselsdato.verdi.plusYears(18) > virkningstidspunkt },
        opplysningsGrunnlag
    )
}