package no.nav.etterlatte.libs.common.soeknad.dataklasser.common

import java.time.LocalDate

data class Opplysning<T>(
    val svar: T,
    val spoersmaal: String? = null
)

data class BetingetOpplysning<T, R>(
    val svar: T,
    val spoersmaal: String? = null,
    val opplysning: R?
)

interface Svar {
    val innhold: Any
}

data class FritekstSvar(
    override val innhold: String
) : Svar

data class DatoSvar(
    override val innhold: LocalDate
) : Svar

data class EnumSvar<E : Enum<E>>(
    val verdi: E,
    override val innhold: String
) : Svar

enum class JaNeiVetIkke { JA, NEI, VET_IKKE }