package model.brev

import model.brev.mapper.finnAvdoed
import model.brev.mapper.finnBarn
import model.brev.mapper.finnUtbetalingsinfo
import no.nav.etterlatte.domene.vedtak.Vedtak
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
            saksnummer = vedtak.sak.id.toString(),
            utbetalingsinfo = vedtak.finnUtbetalingsinfo(),
            barn = vedtak.finnBarn(),
            avdoed = vedtak.finnAvdoed(),
            aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår? Nødvendig?
            spraak = Spraak.NB, // todo, må hentes.
            mottaker = Mottaker(navn = "Barn barnesen", adresse = "Testadresse", postnummer = "0000")
        )
    }
}

data class Utbetalingsinfo(val beloep: Double, val kontonummer: String, val virkningsdato: LocalDate)
