package no.nav.etterlatte.gyldigsoeknad.gyldighetsgrunnlag

import no.nav.etterlatte.libs.common.person.FamilieRelasjon

data class InnsenderHarForeldreansvarGrunnlag(
    val familieRelasjon: FamilieRelasjon?,
    val innsender: PersonInfoGyldighet?,
)
