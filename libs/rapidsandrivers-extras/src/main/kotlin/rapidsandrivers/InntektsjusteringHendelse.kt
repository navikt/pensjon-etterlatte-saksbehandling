package rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

object InntektsjusteringHendelseEvents {
    val INNTEKTSAAR = "@inntektsaar"
    val ANTALL = "@antall"
    val EKSLUDERTE_SAKER = "@ekskluderte_saker"
    val SPESIFIKKE_SAKER = "@spesifikke"
}

enum class InntektsjusteringHendelseType : EventnameHendelseType {
    SEND_INFOBREV,
    ;

    override fun lagEventnameForType(): String = "INNTEKTSJUSTERING:${this.name}"
}
