package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class Utenlandstilsnitt(
    val type: UtenlandstilsnittType,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String,
)

enum class UtenlandstilsnittType {
    NASJONAL,
    UTLANDSTILSNITT,
    BOSATT_UTLAND,
}
