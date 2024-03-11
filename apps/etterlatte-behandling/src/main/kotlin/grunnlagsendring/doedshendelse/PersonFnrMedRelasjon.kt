package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.common.klienter.BorSammen

class PersonFnrMedRelasjon(
    val fnr: String,
    val relasjon: Relasjon,
)

data class PersonerBorSammen(
    val avdoedPerson: String,
    val gjenlevendePerson: String,
    val borSammen: BorSammen,
)
