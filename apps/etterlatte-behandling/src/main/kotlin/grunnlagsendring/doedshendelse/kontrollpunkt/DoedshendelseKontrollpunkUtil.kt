package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.math.absoluteValue

fun harFellesBarn(
    avdoed: PersonDTO,
    eps: PersonDTO,
): Boolean = finnBarn(avdoed).intersect(finnBarn(eps).toSet()).isNotEmpty()

fun harBarn(personDTO: PersonDTO): Boolean = personDTO.hentBarn()?.isNotEmpty() ?: false

fun finnBarn(personDTO: PersonDTO): List<Folkeregisteridentifikator> = personDTO.hentBarn() ?: emptyList()

fun safeYearsBetween(
    first: Temporal?,
    second: Temporal?,
): Long {
    if (first == null) {
        return 0L
    }
    if (second == null) {
        return 0L
    }

    return ChronoUnit.YEARS.between(first, second).absoluteValue
}
