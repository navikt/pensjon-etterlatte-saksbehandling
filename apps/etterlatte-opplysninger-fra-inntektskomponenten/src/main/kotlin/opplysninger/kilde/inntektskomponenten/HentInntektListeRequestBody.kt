package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

data class InntektListeIdent(val identifikator: String, val aktoerType: String) // enum

data class HentInntektListeRequestBody(
    val ident: InntektListeIdent,
    val ainntektsfilter: String,
    val maanedFom: String,
    val maanedTom: String,
    val formaal: String
)