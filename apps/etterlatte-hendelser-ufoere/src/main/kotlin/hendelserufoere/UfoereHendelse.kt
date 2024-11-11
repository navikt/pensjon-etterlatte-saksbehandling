package no.nav.etterlatte.hendelserufoere

class UfoereHendelse {
    var personidentifikator: String = ""
    var ytelse: String? = null
    var virkningstidspunkt: String? = null
    var alderVedVirkningstidspunkt: Int = 0
    var hendelsestype: String? = null
}

data class UfoeretrygdHendelse(
    val hendelseId: Long,
    val ident: String,
)
