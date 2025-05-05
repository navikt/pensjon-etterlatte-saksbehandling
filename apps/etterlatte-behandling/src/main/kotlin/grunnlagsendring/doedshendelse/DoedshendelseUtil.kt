package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType.UTENLANDSKADRESSE
import no.nav.etterlatte.libs.common.person.AdresseType.UTENLANDSKADRESSEFRITTFORMAT
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.math.absoluteValue

fun PersonDoedshendelseDto.under18aarPaaDato(dato: LocalDate): Boolean {
    if (foedselsdato == null && foedselsaar == null) {
        // Vi er usikker så vi lander på "den sikre siden"
        return true
    }
    val aar18 = 18
    val benyttetFoedselsdato = foedselsdato?.verdi ?: LocalDate.of(foedselsaar!!.verdi, 12, 31)

    return ChronoUnit.YEARS.between(benyttetFoedselsdato, dato).absoluteValue < aar18
}

fun harAktivAdresse(person: PersonDoedshendelseDto): Boolean = person.alleAdresser().any { it.aktiv }

fun personBorIUtlandet(person: PersonDoedshendelseDto): Boolean {
    val alleAdresser = person.alleAdresser()
    return if (alleAdresser.any { it.aktiv }) {
        alleAdresser
            .filter { it.aktiv }
            .sortedByDescending { it.gyldigFraOgMed }
            .first()
            .erUtland()
    } else {
        alleAdresser
            .filter { !it.aktiv }
            .sortedByDescending { it.gyldigFraOgMed }
            .first()
            .erUtland()
    }
}

private fun PersonDoedshendelseDto.alleAdresser(): List<Adresse> =
    this.let { person ->
        val kontaktadresse = person.kontaktadresse?.let { opplysningsDtoListe -> opplysningsDtoListe.map { it.verdi } } ?: emptyList()
        val bostedsadresse = person.bostedsadresse?.let { opplysningsDtoListe -> opplysningsDtoListe.map { it.verdi } } ?: emptyList()
        val oppholdsadresse = person.oppholdsadresse?.let { opplysningsDtoListe -> opplysningsDtoListe.map { it.verdi } } ?: emptyList()

        kontaktadresse + bostedsadresse + oppholdsadresse
    }

private fun Adresse.erUtland(): Boolean = this.type in listOf(UTENLANDSKADRESSE, UTENLANDSKADRESSEFRITTFORMAT) && this.land != "NOR"

fun harFellesBarn(
    avdoed: PersonDoedshendelseDto,
    eps: PersonDoedshendelseDto,
): Boolean = finnBarn(avdoed).intersect(finnBarn(eps).toSet()).isNotEmpty()

fun harBarn(personDTO: PersonDoedshendelseDto): Boolean = personDTO.hentBarn()?.isNotEmpty() ?: false

fun finnBarn(personDTO: PersonDoedshendelseDto): List<Folkeregisteridentifikator> = personDTO.hentBarn() ?: emptyList()

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
    avdoed: PersonDoedshendelseDto,
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
                return it.relatertVedSiviltilstand?.value == eps && !harSkiltSivilstandUtenGyldigFomDato(avdoed)
            }
            return false
        }
        ?: false

// Finner siste sivilstand, og returnerer fnr dersom det er en ektefelle/partner, og det ikke finnes en skilsmisse uten gyldig fom-dato
fun finnEktefelleSafe(person: PersonDoedshendelseDto): String? =
    person.sivilstand
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
                ) &&
                !harSkiltSivilstandUtenGyldigFomDato(person)
            ) {
                return it.relatertVedSiviltilstand?.value
            }
            return null
        }

fun finnAntallAarGiftVedDoedsfall(
    avdoed: PersonDoedshendelseDto,
    eps: PersonDoedshendelseDto,
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
        }?.sortedBy { it.gyldigFraOgMed }
        ?.firstOrNull()
        ?.let {
            if (it.gyldigFraOgMed != null) {
                safeYearsBetween(it.gyldigFraOgMed, avdoed.doedsdato!!.verdi)
            } else {
                null
            }
        }

fun harSkiltSivilstandUtenGyldigFomDato(person: PersonDoedshendelseDto): Boolean =
    person.sivilstand
        ?.map { it.verdi }
        ?.filter { it.sivilstatus in listOf(Sivilstatus.SKILT, Sivilstatus.SKILT_PARTNER) }
        ?.any { it.gyldigFraOgMed == null }
        ?: false
