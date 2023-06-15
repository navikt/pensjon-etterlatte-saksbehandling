package no.nav.etterlatte.libs.common.behandling

enum class JaNei { JA, NEI }

data class JaNeiMedBegrunnelse(val svar: JaNei, val begrunnelse: String) {
    fun erJa() = svar == JaNei.JA
}