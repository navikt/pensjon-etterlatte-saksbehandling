package no.nav.etterlatte.personweb.dto

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import java.time.LocalDate

data class Personopplysninger(
    val soeker: Soeker,
)

data class Soeker(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsdato: LocalDate,
    val bostedsadresse: List<Bostedsadresse>,
    val sivilstand: List<Sivilstand>,
    val statsborgerskap: String,
    val pdlStatsborgerskap: List<PdlStatsborgerskap>,
    val utland: Utland,
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
