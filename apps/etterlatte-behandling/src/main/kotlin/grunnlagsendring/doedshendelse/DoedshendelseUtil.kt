package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.math.absoluteValue

fun Person.under18aarPaaDato(dato: LocalDate): Boolean {
    val aar18 = 18
    val benyttetFoedselsdato = foedselsdato ?: LocalDate.of(foedselsaar, 12, 31)

    return ChronoUnit.YEARS.between(benyttetFoedselsdato, dato).absoluteValue < aar18
}

fun personBorIUtlandet(person: PersonDTO): Boolean {
    val person = person.toPerson()
    val kontaktadresse = person.kontaktadresse ?: emptyList()
    val bostedsadresse = person.bostedsadresse ?: emptyList()
    val oppholdsadresse = person.oppholdsadresse ?: emptyList()
    val adresserforPerson = kontaktadresse + bostedsadresse + oppholdsadresse
    val harAktivAdresse = adresserforPerson.any { it.aktiv }
    return if (harAktivAdresse) {
        val adresse = adresserforPerson.filter { it.aktiv }.sortedByDescending { it.gyldigFraOgMed }.first()
        borIUtlandet(adresse)
    } else {
        val adresse = adresserforPerson.filter { !it.aktiv }.sortedByDescending { it.gyldigFraOgMed }.first()
        borIUtlandet(adresse)
    }
}

private fun borIUtlandet(adresse: Adresse): Boolean {
    return adresse.land == null || adresse.land?.uppercase() != "NOR"
}

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
