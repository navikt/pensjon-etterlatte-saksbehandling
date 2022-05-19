package model.brev

import no.nav.etterlatte.libs.common.brev.model.Vedtak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.model.brev.BrevRequest
import no.nav.etterlatte.model.brev.Mottaker
import java.time.LocalDate

data class InnvilgetBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val barn: Barn,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val mottaker: Mottaker
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
            aktuelleParagrafer = vedtak.vilkaar,
            spraak = Spraak.NB,
            mottaker = Mottaker(navn = vedtak.barn.navn, adresse = "Testadresse", postnummer = "0000")
        )
    }
}

data class Utbetalingsinfo(val beloep: Double, val kontonummer: String, val virkningsdato: LocalDate)
