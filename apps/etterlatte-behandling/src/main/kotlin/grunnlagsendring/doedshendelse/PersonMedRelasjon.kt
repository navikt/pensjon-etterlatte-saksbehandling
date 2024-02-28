package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.libs.common.person.Person

class PersonMedRelasjon(
    val person: Person,
    val relasjon: Relasjon,
)

class PersonFnrMedRelasjon(
    val fnr: String,
    val relasjon: Relasjon,
)
