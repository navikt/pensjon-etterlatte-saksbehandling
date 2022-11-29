package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.model.mapper.finnUtbetalingsinfo
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnlag.Persongalleri
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import java.math.BigDecimal
import java.time.LocalDate

data class InnvilgetBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnbeloep: Int,
    val barn: Soeker,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(
            vedtak: Vedtak,
            grunnlag: Persongalleri,
            avsender: Avsender,
            mottaker: Mottaker,
            grunnbeloep: Grunnbeloep
        ): InnvilgetBrevRequest =
            InnvilgetBrevRequest(
                saksnummer = vedtak.sak.id.toString(),
                utbetalingsinfo = vedtak.finnUtbetalingsinfo(),
                grunnbeloep = grunnbeloep.grunnbeloep,
                barn = grunnlag.soeker,
                avdoed = grunnlag.avdoed,
                aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår? Nødvendig?
                spraak = Spraak.NB, // todo, må hentes.
                mottaker = mottaker,
                avsender = avsender
            )
    }
}

data class Utbetalingsinfo(val beloep: BigDecimal, val kontonummer: String, val virkningsdato: LocalDate)
