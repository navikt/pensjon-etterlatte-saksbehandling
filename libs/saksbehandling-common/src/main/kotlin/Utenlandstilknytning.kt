package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class Utenlandstilknytning(
    val type: UtenlandstilknytningType,
    val kilde: Grunnlagsopplysning.Kilde,
    val begrunnelse: String,
)

enum class UtenlandstilknytningType {
    NASJONAL,
    UTLANDSTILSNITT,
    BOSATT_UTLAND,
}
