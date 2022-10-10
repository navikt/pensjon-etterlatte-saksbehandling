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
    soeker: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?,
    virkningstidspunkt: LocalDate?
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(soeker, avdoed, virkningstidspunkt)

    return VurdertVilkaar(
        Vilkaartyper.SOEKER_ER_UNDER_20,
        setVilkaarVurderingFraKriterier(listOf(soekerErUnder20)),
        null,
        listOf(soekerErUnder20),
        LocalDateTime.now()
    )
}
fun kriterieSoekerErUnder20(
    soeker: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?,
    virkningstidspunkt: LocalDate?
): Kriterie {
    val soekerFoedselsdato = soeker?.hentFoedselsdato()
    val soekerFoedselsnummer = soeker?.hentFoedselsnummer()
    val avdoedDoedsdato = avdoed?.hentDoedsdato()
    val avdoedFoedselsnummer = avdoed?.hentFoedselsnummer()

    if (
        soekerFoedselsdato == null ||
        soekerFoedselsnummer == null ||
        virkningstidspunkt == null ||
        avdoedDoedsdato == null ||
        avdoedFoedselsnummer == null
    ) {
        return Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            emptyList()
        )
    }

    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(
            soekerFoedselsdato.id,
            KriterieOpplysningsType.FOEDSELSDATO,
            soekerFoedselsdato.kilde,
            Foedselsdato(soekerFoedselsdato.verdi, soekerFoedselsnummer.verdi)
        ),
        Kriteriegrunnlag(
            avdoedDoedsdato.id,
            KriterieOpplysningsType.DOEDSDATO,
            avdoedDoedsdato.kilde,
            Doedsdato(avdoedDoedsdato.verdi, avdoedFoedselsnummer.verdi)
        )
    )

    return Kriterie(
        Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
        vurderOpplysning { soekerFoedselsdato.verdi.plusYears(18) > virkningstidspunkt },
        opplysningsGrunnlag
    )
}