package no.nav.etterlatte.personweb.dto

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate
import java.time.LocalDateTime

data class Personopplysninger(
    val soeker: PersonopplysningPerson?,
    val avdoede: List<PersonopplysningPerson?>,
    val gjenlevende: List<PersonopplysningPerson?>,
)

data class PersonopplysningPerson(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsdato: LocalDate?,
    val doedsdato: LocalDate?,
    val bostedsadresse: List<Bostedsadresse>?,
    val sivilstand: List<Sivilstand>?,
    val statsborgerskap: String?,
    val pdlStatsborgerskap: List<PdlStatsborgerskap>?,
    val utland: Utland?,
    val familierelasjon: Familierelasjon?,
    val avdoedesBarn: List<PersonopplysningPerson?>?,
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>?,
)

data class Bostedsadresse(
    val adresse: String?,
    val postnr: String?,
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
    val aktiv: Boolean,
)

data class Sivilstand(
    val sivilstatus: Sivilstatus,
    val relatertVedSivilstand: Folkeregisteridentifikator?,
    val gyldigFraOgMed: LocalDate?,
)

data class PdlStatsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
)

data class Familierelasjon(
    val ansvarligeForeldre: List<Folkeregisteridentifikator>?,
    val barn: List<Folkeregisteridentifikator>?,
)
