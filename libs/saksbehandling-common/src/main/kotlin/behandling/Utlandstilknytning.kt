package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class Utlandstilknytning(
    val type: UtlandstilknytningType,
    val kilde: Grunnlagsopplysning.Kilde,
    val begrunnelse: String,
) {
    fun erBosattUtland() = this.type.erBosattUtland()
}

enum class UtlandstilknytningType {
    NASJONAL,
    UTLANDSTILSNITT,
    BOSATT_UTLAND,
}

fun UtlandstilknytningType?.erBosattUtland() = this == UtlandstilknytningType.BOSATT_UTLAND
