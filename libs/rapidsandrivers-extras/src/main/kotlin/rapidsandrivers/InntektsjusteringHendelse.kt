package rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

object InntektsjusteringHendelseEvents {
    val inntektsaar = "@inntektsaar"
}

enum class InntektsjusteringHendelseType : EventnameHendelseType {
    SEND_VARSEL,
    ;

    override fun lagEventnameForType(): String = "INNTEKTSJUSTERING:${this.name}"
}
