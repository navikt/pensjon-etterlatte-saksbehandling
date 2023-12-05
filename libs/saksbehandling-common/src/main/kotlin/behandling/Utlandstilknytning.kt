package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class Utlandstilknytning(
    val type: UtlandstilknytningType,
    val kilde: Grunnlagsopplysning.Kilde,
    val begrunnelse: String,
)

enum class UtlandstilknytningType {
    NASJONAL,
    UTLANDSTILSNITT,
    BOSATT_UTLAND,
}
