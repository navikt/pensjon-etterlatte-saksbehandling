package no.nav.etterlatte.libs.common.event

const val BREVMAL_RIVER_KEY = "brevmal"

enum class BrevRequestHendelseType : EventnameHendelseType {
    OPPRETT_BREV,
    OPPRETT_JOURNALFOER_OG_DISTRIBUER,
    ;

    override fun lagEventnameForType(): String = "BREV:${this.name}"
}
