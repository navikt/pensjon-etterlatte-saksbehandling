package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Foedselsnummer

/*
* Beregningsgrunnlag for hvilke soesken som skal tas med i beregningen
* */

data class SoeskenMedIBeregning(val foedselsnummer: Foedselsnummer, val skalBrukes: Boolean)

//  Wrapper class for å kunne serialisere/deseralisere ObjectNode
data class Beregningsgrunnlag(val beregningsgrunnlag: List<SoeskenMedIBeregning>)
