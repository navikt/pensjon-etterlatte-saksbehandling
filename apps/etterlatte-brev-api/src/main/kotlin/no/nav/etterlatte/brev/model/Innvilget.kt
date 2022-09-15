package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.model.mapper.finnAvdoed
import no.nav.etterlatte.brev.model.mapper.finnBarn
import no.nav.etterlatte.brev.model.mapper.finnMottaker
import no.nav.etterlatte.brev.model.mapper.finnUtbetalingsinfo
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.time.LocalDate

data class InnvilgetBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnbeloep: Int,
    val barn: Barn,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(vedtak: Vedtak, avsender: Avsender, grunnbeloep: Grunnbeloep): InnvilgetBrevRequest =
            InnvilgetBrevRequest(
                saksnummer = vedtak.sak.id.toString(),
                utbetalingsinfo = vedtak.finnUtbetalingsinfo(),
                grunnbeloep = grunnbeloep.grunnbeloep,
                barn = vedtak.finnBarn(),
                avdoed = vedtak.finnAvdoed(),
                aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår? Nødvendig?
                spraak = Spraak.NB, // todo, må hentes.
                mottaker = vedtak.finnMottaker(),
                avsender = avsender
            )
    }
}

data class Utbetalingsinfo(val beloep: Double, val kontonummer: String, val virkningsdato: LocalDate)