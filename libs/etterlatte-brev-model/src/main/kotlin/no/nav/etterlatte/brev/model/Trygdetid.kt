package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.UgyldigBeregningsMetode
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.time.LocalDate

fun List<LandDto>.hentLandFraLandkode(landkode: String) = firstOrNull { it.isoLandkode == landkode }?.beskrivelse?.tekst

fun TrygdetidDto.fromDto(
    beregningsMetodeAnvendt: BeregningsMetode,
    beregningsMetodeFraGrunnlag: BeregningsMetode,
    navnAvdoed: String?,
    landKodeverk: List<LandDto>,
) = TrygdetidMedBeregningsmetode(
    navnAvdoed = navnAvdoed,
    trygdetidsperioder =
        when (beregningsMetodeAnvendt) {
            BeregningsMetode.NASJONAL -> {
                trygdetidGrunnlag.filter { it.bosted == LandNormalisert.NORGE.isoCode }
            }

            BeregningsMetode.PRORATA -> {
                // Kun ta med de som er avtaleland (Norge er alltid avtaleland)
                trygdetidGrunnlag.filter { it.prorata || it.bosted == LandNormalisert.NORGE.isoCode }
            }

            else -> {
                throw IllegalArgumentException("$beregningsMetodeAnvendt er ikke en gyldig beregningsmetode")
            }
        }.map { grunnlag ->
            Trygdetidsperiode(
                datoFOM = grunnlag.periodeFra,
                datoTOM = grunnlag.periodeTil,
                landkode = grunnlag.bosted,
                land = landKodeverk.hentLandFraLandkode(grunnlag.bosted),
                opptjeningsperiode = grunnlag.beregnet,
                type = TrygdetidType.valueOf(grunnlag.type),
            )
        },
    beregnetTrygdetidAar =
        when (beregningsMetodeAnvendt) {
            BeregningsMetode.NASJONAL -> {
                beregnetTrygdetid?.resultat?.samletTrygdetidNorge
                    ?: throw ManglerMedTrygdetidVeBrukIBrev()
            }

            BeregningsMetode.PRORATA -> {
                beregnetTrygdetid?.resultat?.samletTrygdetidTeoretisk
                    ?: throw ManglerMedTrygdetidVeBrukIBrev()
            }

            BeregningsMetode.BEST -> {
                throw UgyldigBeregningsMetode()
            }

            else -> {
                throw ManglerMedTrygdetidVeBrukIBrev()
            }
        },
    prorataBroek = beregnetTrygdetid?.resultat?.prorataBroek,
    mindreEnnFireFemtedelerAvOpptjeningstiden =
        beregnetTrygdetid
            ?.resultat
            ?.fremtidigTrygdetidNorge
            ?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
    beregningsMetodeFraGrunnlag = beregningsMetodeFraGrunnlag,
    beregningsMetodeAnvendt = beregningsMetodeAnvendt,
    ident = this.ident,
)

fun TrygdetidDto.erYrkesskade() = beregnetTrygdetid?.resultat?.yrkesskade ?: false

data class Trygdetidsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val landkode: String,
    val land: String?,
    val opptjeningsperiode: BeregnetTrygdetidGrunnlagDto?,
    val type: TrygdetidType,
)

data class TrygdetidMedBeregningsmetode(
    val navnAvdoed: String?,
    val trygdetidsperioder: List<Trygdetidsperiode>,
    val beregnetTrygdetidAar: Int,
    val prorataBroek: IntBroek?,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
    val ident: String,
)
