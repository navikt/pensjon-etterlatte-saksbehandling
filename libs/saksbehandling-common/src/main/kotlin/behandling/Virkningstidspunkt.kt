package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth

data class Virkningstidspunkt(
    val dato: YearMonth,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String,
    val kravdato: LocalDate? = null,
) {
    companion object {
        fun create(
            dato: YearMonth,
            begrunnelse: String,
            kravdato: LocalDate? = null,
            saksbehandler: Grunnlagsopplysning.Saksbehandler,
        ) = Virkningstidspunkt(dato, saksbehandler, begrunnelse, kravdato)
    }
}

fun LocalDate.tilVirkningstidspunkt(begrunnelse: String) =
    Virkningstidspunkt(
        YearMonth.from(this),
        Grunnlagsopplysning.automatiskSaksbehandler,
        begrunnelse,
    )

fun Virkningstidspunkt.erPaaNyttRegelverk() = this.dato >= YearMonth.of(2024, 1)
