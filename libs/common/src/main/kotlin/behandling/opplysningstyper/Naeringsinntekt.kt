package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Naeringsinntekt(
    val selvstendigNaeringsdrivende: Svar?,
    val haddeNaeringsinntektVedDoedsfall: Svar?,
    val naeringsinntektAarFoerDoedsfall: String?,
    val foedselsnummer: String
)