package no.nav.etterlatte.samordning.vedtak

import java.time.LocalDate
import java.time.LocalDateTime

data class SamhandlerPersonDto(
    val fnr: String,
    val forhold: List<TjenestepensjonForhold> = emptyList(),
    val changeStamp: ChangeStampDateDto? = null,
)

class TjenestepensjonForhold(
    val tpNr: String,
    val tpOrdningNavn: String,
    val ytelser: List<SamhandlerYtelseDto> = emptyList(),
    val datoSistOpptjening: LocalDate? = null,
    val sistEndretDatoSistOpptjening: LocalDateTime? = null,
    val harGjenlevendeYtelse: Boolean? = null,
    val kilde: KildeTypeCode,
    val changeStamp: ChangeStampDateDto? = null,
)

data class SamhandlerYtelseDto(
    val datoInnmeldtYtelseFom: LocalDate? = null,
    val ytelseType: YtelseTypeCode,
    val datoYtelseIverksattFom: LocalDate? = null,
    val datoYtelseIverksattTom: LocalDate? = null,
    val changeStamp: ChangeStampDateDto? = null,
)

enum class KildeTypeCode(
    val decode: String,
    val createdBy: String,
    val createdDate: LocalDate,
    val updatedBy: String,
    val updatedDate: LocalDate,
)

enum class YtelseTypeCode(
    val createdBy: String,
    val createdDate: LocalDate,
    val updatedBy: String,
    val updatedDate: LocalDate,
)

data class ChangeStampDateDto(
    val createdBy: String,
    val createdDate: LocalDateTime,
    val updatedBy: String,
    val updatedDate: LocalDateTime,
)

class TjenestepensjonManglendeTilgangException(override val message: String) : Exception(message)
