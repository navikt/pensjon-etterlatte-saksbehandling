package model.brev

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
            utbetalingsinfo = Utbetalingsinfo(
                beloep = vedtak.pensjonTilUtbetaling!![0].beloep!!.toDouble(),
                virkningsdato = LocalDate.of(vedtak.virk.fom.year, vedtak.virk.fom.month, 1),
                kontonummer = "<todo: Ikke tilgjengelig>"
            ),
            barn = Barn(
                navn = "Ola nordmann", // todo: Hentes fra pdl/grunnlag
                fnr = vedtak.sak.ident,
            ),
            avdoed = Avdoed("Gammel, mann", LocalDate.now()), // todo: Hentes fra behandling/grunnlag.
            aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår? Nødvendig?
            spraak = Spraak.NB, // todo, må hentes.
            mottaker = Mottaker(navn = "Barn barnesen", adresse = "Testadresse", postnummer = "0000")
        )
    }
}

data class Utbetalingsinfo(val beloep: Double, val kontonummer: String, val virkningsdato: LocalDate)
