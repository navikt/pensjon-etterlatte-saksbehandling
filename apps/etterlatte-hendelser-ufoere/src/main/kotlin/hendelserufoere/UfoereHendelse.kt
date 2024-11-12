package no.nav.etterlatte.hendelserufoere

data class UfoereHendelse(
    val personIdent: String,
    val fodselsdato: String,
    val virkningsdato: String,
    val vedtaksType: String,
)
