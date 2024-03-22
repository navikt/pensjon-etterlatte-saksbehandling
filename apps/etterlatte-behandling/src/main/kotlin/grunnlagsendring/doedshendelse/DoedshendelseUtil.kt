package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstatus
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

fun varEktefelleVedDoedsfall(
    avdoed: PersonDTO,
    eps: String,
): Boolean =
    avdoed.sivilstand
        ?.asSequence()
        ?.map { it.verdi }
        ?.sortedBy { it.gyldigFraOgMed }
        ?.lastOrNull()
        ?.let {
            if (it.sivilstatus in
                listOf(
                    Sivilstatus.GIFT,
                    Sivilstatus.SEPARERT,
                    Sivilstatus.REGISTRERT_PARTNER,
                    Sivilstatus.SEPARERT_PARTNER,
                )
            ) {
                return it.relatertVedSiviltilstand?.value == eps
            }
            return false
        }
        ?: false

fun finnAntallAarGiftVedDoedsfall(
    avdoed: PersonDTO,
    eps: PersonDTO,
): Long? =
    avdoed.sivilstand
        ?.asSequence()
        ?.map { it.verdi }
        ?.filter { it.relatertVedSiviltilstand == eps.foedselsnummer.verdi }
        ?.filter {
            it.sivilstatus in
                listOf(
                    Sivilstatus.GIFT,
                    Sivilstatus.SEPARERT,
                    Sivilstatus.REGISTRERT_PARTNER,
                    Sivilstatus.SEPARERT_PARTNER,
                )
        }
        ?.sortedBy { it.gyldigFraOgMed }
        ?.firstOrNull()
        ?.let {
            if (it.gyldigFraOgMed != null) {
                safeYearsBetween(it.gyldigFraOgMed, avdoed.doedsdato!!.verdi)
            } else {
                null
            }
        }
