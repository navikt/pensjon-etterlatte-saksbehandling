package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth

data class Virkningstidspunkt(
    val dato: YearMonth,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String
) {
    companion object {
        fun create(dato: YearMonth, ident: String, begrunnelse: String) =
            Virkningstidspunkt(dato, Grunnlagsopplysning.Saksbehandler.create(ident), begrunnelse)
    }
}

fun LocalDate.tilVirkningstidspunkt(begrunnelse: String) =
    Virkningstidspunkt(
        YearMonth.from(this),
        Grunnlagsopplysning.automatiskSaksbehandler,
        begrunnelse
    )