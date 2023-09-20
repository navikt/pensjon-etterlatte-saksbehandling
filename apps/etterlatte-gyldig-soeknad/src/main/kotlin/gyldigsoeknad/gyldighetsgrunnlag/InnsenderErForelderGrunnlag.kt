package no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper

import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.person.FamilieRelasjon

data class InnsenderErForelderGrunnlag(
    val familieRelasjon: FamilieRelasjon?,
    val innsender: PersonInfoGyldighet?,
    val gjenlevende: List<PersonInfoGyldighet?>,
)
