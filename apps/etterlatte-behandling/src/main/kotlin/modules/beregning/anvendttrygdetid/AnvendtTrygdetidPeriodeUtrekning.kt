package no.nav.etterlatte.modules.beregning.anvendttrygdetid

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.libs.regler.PeriodisertResultat

data class AnvendtTrygdetidPeriodeUtrekning(
    val anvendt: List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>>,
    val utrekning: List<PeriodisertResultat<AnvendtTrygdetid>>,
)
