package no.nav.etterlatte.libs.common.soeknad.dataklasser.common

data class Opplysning<T>(
    val svar: T,
    val spoersmaal: String? = null,
)

data class BetingetOpplysning<T, R>(
    val svar: T,
    val spoersmaal: String? = null,
    val opplysning: R?,
)

enum class Svar { JA, NEI, VET_IKKE }
