package rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

object InntektsjusteringHendelseEvents {
    val inntektsaar = "@inntektsaar"
    val ekskluderte_saker = "@ekskluderte_saker"
}

enum class InntektsjusteringHendelseType : EventnameHendelseType {
    SEND_INFOBREV,
    ;

    override fun lagEventnameForType(): String = "INNTEKTSJUSTERING:${this.name}"
}
