package no.nav.etterlatte.brev.model.mapper

import no.nav.etterlatte.brev.model.Utbetalingsinfo
import no.nav.etterlatte.domene.vedtak.Vedtak
import java.time.LocalDate

fun Vedtak.finnUtbetalingsinfo() = Utbetalingsinfo(
    beloep = this.pensjonTilUtbetaling!![0].beloep!!.toDouble(),
    virkningsdato = LocalDate.of(this.virk.fom.year, this.virk.fom.month, 1),
    kontonummer = "<todo: Ikke tilgjengelig>"
)