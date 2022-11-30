package no.nav.etterlatte.brev.model.mapper

import no.nav.etterlatte.brev.model.Utbetalingsinfo
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import java.time.LocalDate

fun Vedtak.finnUtbetalingsinfo() = Utbetalingsinfo(
    beloep = this.beregning!!.sammendrag[0].beloep,
    virkningsdato = LocalDate.of(this.virk.fom.year, this.virk.fom.month, 1),
    kontonummer = "<todo: Ikke tilgjengelig>"
)
