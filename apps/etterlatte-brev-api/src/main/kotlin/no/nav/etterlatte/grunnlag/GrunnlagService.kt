package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.brev.model.Avdoed
import no.nav.etterlatte.brev.model.Innsender
import no.nav.etterlatte.brev.model.Soeker
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import org.slf4j.LoggerFactory

class GrunnlagService(private val klient: GrunnlagKlient) {

    suspend fun hentGrunnlag(sakid: Long, accessToken: String): Persongalleri {
        val grunnlag = klient.hentGrunnlag(sakid, accessToken)
        val innsenderGrunnlag = klient.hentGrunnlag(sakid, Opplysningstype.INNSENDER_SOEKNAD_V1, accessToken)

        return Persongalleri(
            innsender = innsenderGrunnlag.mapInnsender(),
            soeker = grunnlag.mapSoeker(),
            avdoed = grunnlag.mapAvdoed()
        )
    }

}

data class Persongalleri(
    val innsender: Innsender,
    val soeker: Soeker,
    val avdoed: Avdoed
)

private fun Grunnlagsopplysning<InnsenderSoeknad>.mapInnsender(): Innsender = with(this.opplysning) {
    Innsender(
        navn = "$fornavn $etternavn",
        fnr = foedselsnummer.value
    )
}

private fun Grunnlag.mapSoeker(): Soeker = with(this.soeker) {
    Soeker(
        navn = hentNavn()!!.verdi.let { "${it.fornavn} ${it.etternavn}" },
        fnr = hentFoedselsnummer()!!.verdi.value
    )
}

private fun Grunnlag.mapAvdoed(): Avdoed = with(this.familie) {
    val avdoed = hentAvdoed()

    Avdoed(
        navn = avdoed.hentNavn()!!.verdi.let { "${it.fornavn} ${it.etternavn}" },
        doedsdato = avdoed.hentDoedsdato()!!.verdi!!
    )
}
