package no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag

import no.nav.etterlatte.libs.common.person.Siviltilstand

data class InnsenderErGjenlevende(
    val innsender: PersonInfoGyldighet?,
    val avdoed: List<PersonInfoMedSiviltilstand>?
)

data class PersonInfoMedSiviltilstand(
    val personInfo: PersonInfoGyldighet?,
    val siviltilstand: List<Siviltilstand>?
)