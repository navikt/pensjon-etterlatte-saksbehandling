package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.Avdoed
import no.nav.etterlatte.brev.model.Innsender
import no.nav.etterlatte.brev.model.Soeker
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad

data class Persongalleri(
    val innsender: Innsender,
    val soeker: Soeker,
    val avdoed: Avdoed
)

fun Grunnlagsopplysning<InnsenderSoeknad>.mapInnsender(): Innsender = with(this.opplysning) {
    Innsender(
        navn = "$fornavn $etternavn",
        fnr = foedselsnummer.value
    )
}

fun Grunnlag.mapSoeker(): Soeker = with(this.soeker) {
    Soeker(
        navn = hentNavn()!!.verdi.let { "${it.fornavn} ${it.etternavn}" },
        fnr = hentFoedselsnummer()!!.verdi.value
    )
}

fun Grunnlag.mapAvdoed(): Avdoed = with(this.familie) {
    val avdoed = hentAvdoed()

    Avdoed(
        navn = avdoed.hentNavn()!!.verdi.let { "${it.fornavn} ${it.etternavn}" },
        doedsdato = avdoed.hentDoedsdato()!!.verdi!!
    )
}
