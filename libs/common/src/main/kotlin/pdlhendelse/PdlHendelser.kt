package no.nav.etterlatte.libs.common.pdlhendelse

import java.time.LocalDate

sealed interface PdlHendelse

data class Doedshendelse(
    val avdoedFnr: String,
    val doedsdato: LocalDate?,
    val endringstype: Endringstype
) : PdlHendelse

fun Doedshendelse.erLik(other: Doedshendelse) =
    this.avdoedFnr == other.avdoedFnr && this.doedsdato == other.doedsdato

data class UtflyttingsHendelse(
    val fnr: String,
    val tilflyttingsLand: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?,
    val endringstype: Endringstype
) : PdlHendelse

fun UtflyttingsHendelse.erLik(other: UtflyttingsHendelse) =
    this.fnr == other.fnr &&
        this.tilflyttingsLand == other.tilflyttingsLand &&
        this.utflyttingsdato == other.utflyttingsdato

data class ForelderBarnRelasjonHendelse(
    val fnr: String,
    val relatertPersonsIdent: String?,
    val relatertPersonsRolle: String?,
    val minRolleForPerson: String?,
    val relatertPersonUtenFolkeregisteridentifikator: String?,
    val endringstype: Endringstype
) : PdlHendelse

fun ForelderBarnRelasjonHendelse.erLik(other: ForelderBarnRelasjonHendelse) =
    this.fnr == other.fnr &&
        if (this.relatertPersonsIdent != null) {
            this.relatertPersonsIdent == other.relatertPersonsIdent
        } else {
            this.relatertPersonUtenFolkeregisteridentifikator == other.relatertPersonUtenFolkeregisteridentifikator
        }

enum class Endringstype {
    OPPRETTET, KORRIGERT, ANNULLERT, OPPHOERT
}