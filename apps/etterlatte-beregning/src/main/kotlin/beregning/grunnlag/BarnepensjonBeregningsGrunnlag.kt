package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning

data class BarnepensjonBeregningsGrunnlag(
    val soeskenMedIBeregning: List<SoeskenMedIBeregning>,
    val institusjonsopphold: Institusjonsopphold
)