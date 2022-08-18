package no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag

import no.nav.etterlatte.libs.common.person.FamilieRelasjon

data class InnsenderHarForeldreansvarGrunnlag(
    val familieRelasjon: FamilieRelasjon?,
    val innsender: PersonInfoGyldighet?
)