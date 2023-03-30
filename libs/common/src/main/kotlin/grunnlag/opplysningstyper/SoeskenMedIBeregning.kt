package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

/*
* Beregningsgrunnlag for hvilke soesken som skal tas med i beregningen
* */

data class SoeskenMedIBeregning(val folkeregisteridentifikator: Folkeregisteridentifikator, val skalBrukes: Boolean)

//  Wrapper class for å kunne serialisere/deseralisere ObjectNode
data class Beregningsgrunnlag(val beregningsgrunnlag: List<SoeskenMedIBeregning>)