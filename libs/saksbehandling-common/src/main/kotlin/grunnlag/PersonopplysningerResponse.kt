package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.behandling.AnnenForelder

data class PersonopplysningerResponse(
    val innsender: Personopplysning?,
    val soeker: Personopplysning?,
    val avdoede: List<Personopplysning>,
    val gjenlevende: List<Personopplysning>,
    val annenForelder: AnnenForelder?,
)
