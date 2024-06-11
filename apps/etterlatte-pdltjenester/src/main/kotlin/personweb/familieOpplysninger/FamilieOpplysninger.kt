package no.nav.etterlatte.personweb.familieOpplysninger

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Statsborgerskap
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate
import java.time.LocalDateTime

data class FamilieOpplysninger(
    val soeker: Familiemedlem?,
    val avdoede: List<Familiemedlem?>,
    val gjenlevende: List<Familiemedlem?>,
)

data class Familiemedlem(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsdato: LocalDate?,
    val doedsdato: LocalDate?,
    val bostedsadresse: List<Bostedsadresse>?,
    val sivilstand: List<Sivilstand>?,
    val statsborgerskap: String?,
    val pdlStatsborgerskap: List<Statsborgerskap>?,
    val utland: Utland?,
    val familierelasjon: Familierelasjon?,
    val barn: List<Familiemedlem?>?,
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

data class Familierelasjon(
    val ansvarligeForeldre: List<Folkeregisteridentifikator>?,
    val foreldre: List<Folkeregisteridentifikator>?,
    val barn: List<Folkeregisteridentifikator>?,
)
