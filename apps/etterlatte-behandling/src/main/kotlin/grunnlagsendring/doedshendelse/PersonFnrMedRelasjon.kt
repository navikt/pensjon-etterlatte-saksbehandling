package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto

data class PersonFnrMedRelasjon(
    val fnr: String,
    val relasjon: Relasjon,
)

data class AvdoedOgAnnenForelderMedFellesbarn(
    val avdoedPerson: PersonDoedshendelseDto,
    val gjenlevendeForelder: PersonDoedshendelseDto,
)
