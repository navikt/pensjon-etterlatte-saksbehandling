package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

data class HendelseslisteFraSkatt(
    val hendelser: List<SkatteoppgjoerHendelse>,
) {
    companion object {
        fun stub(
            startSekvensnummer: Long = 0,
            antall: Int = 10,
            aar: Int = ETTEROPPGJOER_AAR,
        ): HendelseslisteFraSkatt {
            val hendelser =
                List(antall) { index ->
                    SkatteoppgjoerHendelse(
                        gjelderPeriode = aar.toString(),
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
)

data class PensjonsgivendeInntektSummert(
    val loensinntekt: Int,
    val naeringsinntekt: Int,
) {
    val summertInntekt = loensinntekt + naeringsinntekt
}
