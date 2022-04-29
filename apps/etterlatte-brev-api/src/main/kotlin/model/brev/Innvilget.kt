package model.brev

import model.Vedtak
import no.nav.etterlatte.model.brev.BrevRequest
import java.time.LocalDate

data class InnvilgetBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val barn: Barn,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>
) : BrevRequest() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(vedtak: Vedtak): InnvilgetBrevRequest = InnvilgetBrevRequest(
            saksnummer = vedtak.saksnummer,
            utbetalingsinfo = Utbetalingsinfo(
                beloep = vedtak.sum,
                kontonummer = vedtak.kontonummer,
                virkningsdato = vedtak.virkningsdato
            ),
            barn = Barn(
                navn = vedtak.barn.navn,
                fnr = vedtak.barn.fnr,
            ),
            avdoed = Avdoed(
                navn = vedtak.avdoed.navn,
                doedsdato = vedtak.avdoed.doedsdato
            ),
            aktuelleParagrafer = vedtak.vilkaar
        )
    }
}

data class Utbetalingsinfo(val beloep: Double, val kontonummer: String, val virkningsdato: LocalDate)
data class Barn(val navn: String, val fnr: String)
data class Avdoed(val navn: String, val doedsdato: LocalDate)
