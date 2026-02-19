package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

data class HendelseslisteFraSkatt(
    val hendelser: List<SkatteoppgjoerHendelse>,
) {
    // TODO: støtte stub for flere perioder?
    companion object {
        fun stub(
            startSekvensnummer: Long = 0,
            antall: Int = 10,
        ): HendelseslisteFraSkatt {
            val hendelser =
                List(antall) { index ->
                    SkatteoppgjoerHendelse(
                        gjelderPeriode = "2024",
                        hendelsetype = SigrunKlient.HENDELSETYPE_NY,
                        identifikator = index.toString(),
                        sekvensnummer = startSekvensnummer + index,
                        registreringstidspunkt = Tidspunkt.now(),
                    )
                }
            return HendelseslisteFraSkatt(hendelser)
        }
    }
}

data class HendelserSekvensnummerFraSkatt(
    val sekvensnummer: Long,
)

data class SkatteoppgjoerHendelse(
    val gjelderPeriode: String?, // inntektsaar
    val hendelsetype: String?,
    val identifikator: String,
    val sekvensnummer: Long,
    val registreringstidspunkt: Tidspunkt?,
) {
    fun erNyHendelse(): Boolean = hendelsetype == null || hendelsetype == SigrunKlient.HENDELSETYPE_NY
}

data class PensjonsgivendeInntektSummert(
    val loensinntekt: Int,
    val naeringsinntekt: Int,
) {
    val summertInntekt = loensinntekt + naeringsinntekt
}
