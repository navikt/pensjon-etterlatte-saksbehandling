package no.nav.etterlatte.personweb.dto

import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Gjenlevende
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

data class Personopplysninger(
    val soeker: PersonopplysningPerson,
    val avdoed: List<PersonopplysningPerson>,
    val gjenlevende: List<Gjenlevende>,
)

data class PersonopplysningPerson(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsdato: LocalDate,
    val bostedsadresse: List<Bostedsadresse>,
    val sivilstand: List<Sivilstand>,
    val statsborgerskap: String,
    val pdlStatsborgerskap: List<PdlStatsborgerskap>,
    val utland: Utland,
    val familieRelasjon: Familierelasjon,
    val avdoedesBarn: List<PersonopplysningPerson>,
    val vergemaalEllerFremtidsfullmakt: VergemaalEllerFremtidsfullmakt,
)

data class Bostedsadresse(
    val adresse: String,
    val postnr: String,
    val poststed: String,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val aktiv: Boolean,
)

data class Sivilstand(
    val sivilstatus: Sivilstatus,
    val relatertVedSivilstand: Folkeregisteridentifikator,
    val gyldigFraOgMed: LocalDate,
)

data class PdlStatsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
)

data class Familierelasjon(
    val ansvarligeForeldre: List<Folkeregisteridentifikator>,
    val barn: List<Folkeregisteridentifikator>,
)
