package no.nav.etterlatte.libs.common.beregning

import java.util.UUID

data class SanksjonertYtelse(
    val sanksjonId: UUID,
    val sanksjonType: SanksjonType,
)

enum class SanksjonType {
    BORTFALL,
    OPPHOER,
    STANS,
    UTESTENGING,
}
