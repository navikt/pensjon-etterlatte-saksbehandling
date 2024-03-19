package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

fun harFellesBarn(
    avdoed: PersonDTO,
    eps: PersonDTO,
): Boolean = finnBarn(avdoed).intersect(finnBarn(eps)).isNotEmpty()

fun harBarn(personDTO: PersonDTO): Boolean = finnBarn(personDTO).isNotEmpty()

fun finnBarn(personDTO: PersonDTO): List<Folkeregisteridentifikator> = personDTO.familieRelasjon?.verdi?.barn ?: emptyList()
