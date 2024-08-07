package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

/*
* Beregningsgrunnlag for hvilke soesken som skal tas med i beregningen
* */

data class SoeskenMedIBeregning(
    val foedselsnummer: Folkeregisteridentifikator,
    val skalBrukes: Boolean,
)
