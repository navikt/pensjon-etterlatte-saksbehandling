package no.nav.etterlatte.libs.common.sak

data class SakIDListe(val ider: List<Long>) {
    fun contains(id: Long) = ider.contains(id)
}