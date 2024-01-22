package no.nav.etterlatte.gyldigsoeknad.gyldighetsgrunnlag

import no.nav.etterlatte.libs.common.person.FamilieRelasjon

data class InnsenderErForelderGrunnlag(
    val familieRelasjon: FamilieRelasjon?,
    val innsender: PersonInfoGyldighet?,
    val gjenlevende: List<PersonInfoGyldighet?>,
)
