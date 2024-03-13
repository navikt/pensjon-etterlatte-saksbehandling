package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.libs.common.pdl.PersonDTO

class PersonFnrMedRelasjon(
    val fnr: String,
    val relasjon: Relasjon,
)

data class AvdoedOgAnnenForelderMedFellesbarn(
    val avdoedPerson: PersonDTO,
    val gjenlevendeForelder: PersonDTO,
)
