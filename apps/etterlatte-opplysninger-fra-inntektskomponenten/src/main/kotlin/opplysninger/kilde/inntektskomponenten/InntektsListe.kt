package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.libs.common.inntekt.ArbeidsInntektMaaned
import no.nav.etterlatte.libs.common.inntekt.Ident

data class InntektsKomponentenResponse (
    val arbeidsInntektMaaned : List<ArbeidsInntektMaaned>?,
    val ident : Ident
)
