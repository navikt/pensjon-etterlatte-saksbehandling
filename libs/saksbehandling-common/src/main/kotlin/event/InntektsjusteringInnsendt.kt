package no.nav.etterlatte.libs.common.event

object InntektsjusteringInnsendt {
    val fnrBruker = "@fnr_bruker"
    val inntektsjusteringInnhold = "@inntektsjustering_innhold"
    val inntektsaar = "@inntektsaar"
    val dokarkivReturKey = "@dokarkivRetur"
}

enum class InntektsjusteringInnsendtHendelseType(
    val eventname: String,
) : EventnameHendelseType {
    EVENT_NAME_INNSENDT("inntektsjustering_innsendt"),
    ;

    override fun lagEventnameForType(): String = this.eventname
}
