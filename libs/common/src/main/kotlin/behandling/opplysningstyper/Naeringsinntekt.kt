package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Naeringsinntekt(
    val naeringsinntektVedDoedsfall: Svar?,
    val naeringsinntektPrAarFoerDoedsfall: String?,
    val fnr: String
)