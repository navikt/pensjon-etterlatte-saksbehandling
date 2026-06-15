package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto

fun Trygdetid.toDto(): TrygdetidDto =
    TrygdetidDto(
        id = id,
        behandlingId = behandlingId,
        beregnetTrygdetid = beregnetTrygdetid?.toDto(),
        trygdetidGrunnlag = trygdetidGrunnlag.map { it.toDto() },
        opplysninger = this.opplysninger.toDto(),
        overstyrtNorskPoengaar = this.overstyrtNorskPoengaar,
        ident = this.ident,
        opplysningerDifferanse = krevIkkeNull(opplysningerDifferanse) { "Differanseopplysninger mangler" },
        begrunnelse = this.begrunnelse,
    )

private fun DetaljertBeregnetTrygdetid.toDto(): DetaljertBeregnetTrygdetidDto =
    DetaljertBeregnetTrygdetidDto(
        resultat = resultat,
        tidspunkt = tidspunkt,
    )

private fun TrygdetidGrunnlag.toDto(): TrygdetidGrunnlagDto =
    TrygdetidGrunnlagDto(
        id = id,
        type = type.name,
        bosted = bosted,
        periodeFra = periode.fra,
        periodeTil = periode.til,
        beregnet =
            beregnetTrygdetid?.let {
                BeregnetTrygdetidGrunnlagDto(it.verdi.days, it.verdi.months, it.verdi.years)
            },
        kilde =
            when (kilde) {
                is Grunnlagsopplysning.Saksbehandler -> {
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.ident,
                    )
                }

                is Grunnlagsopplysning.Pesys -> {
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.type,
                    )
                }

                is Grunnlagsopplysning.Ufoeretrygd -> {
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.type,
                    )
                }

                is Grunnlagsopplysning.Alderspensjon -> {
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.type,
                    )
                }

                else -> {
                    throw UnsupportedOperationException(
                        "Kilde for trygdetid maa vaere saksbehandler, Ufoeretrygd, Alderspensjon eller pesys, var $kilde",
                    )
                }
            },
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar,
        prorata = prorata,
    )
