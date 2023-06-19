package no.nav.etterlatte.libs.common.pdlhendelse

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import java.time.LocalDate

sealed interface PdlHendelse {
    val hendelseId: String
    val endringstype: Endringstype
    val fnr: String
}

data class VergeMaalEllerFremtidsfullmakt(
    override val hendelseId: String,
    override val endringstype: Endringstype,
    override val fnr: String,
    val vergeIdent: String?
) : PdlHendelse

data class Doedshendelse(
    override val hendelseId: String,
    override val endringstype: Endringstype,
    override val fnr: String,
    val doedsdato: LocalDate?
) : PdlHendelse

data class UtflyttingsHendelse(
    override val hendelseId: String,
    override val endringstype: Endringstype,
    override val fnr: String,
    val tilflyttingsLand: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?
) : PdlHendelse

data class ForelderBarnRelasjonHendelse(
    override val hendelseId: String,
    override val endringstype: Endringstype,
    override val fnr: String,
    val relatertPersonsIdent: String?,
    val relatertPersonsRolle: String?,
    val minRolleForPerson: String?,
    val relatertPersonUtenFolkeregisteridentifikator: String?
) : PdlHendelse

data class Adressebeskyttelse(
    override val hendelseId: String,
    override val endringstype: Endringstype,
    override val fnr: String,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering
) : PdlHendelse

data class SivilstandHendelse(
    override val hendelseId: String,
    override val endringstype: Endringstype,
    override val fnr: String,
    val type: String?,
    val relatertVedSivilstand: String?,
    val gyldigFraOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?
) : PdlHendelse

enum class Endringstype {
    OPPRETTET, KORRIGERT, ANNULLERT, OPPHOERT
}