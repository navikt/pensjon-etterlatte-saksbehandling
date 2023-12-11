package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.time.LocalDate
import java.time.YearMonth

data class EtterbetalingNy(
    val fom: YearMonth,
    val tom: YearMonth,
) {
    init {
        if (fom > tom) {
            throw EtterbetalingException.EtterbetalingFomErEtterTom(fom, tom)
        }
        if (tom > YearMonth.now()) {
            throw EtterbetalingException.EtterbetalingTomErFramITid(tom)
        }
    }

    companion object {
        fun fra(
            datoFom: LocalDate?,
            datoTom: LocalDate?,
        ): EtterbetalingNy {
            if (datoFom == null || datoTom == null) {
                throw EtterbetalingException.EtterbetalingManglerDato()
            }
            return EtterbetalingNy(YearMonth.from(datoFom), YearMonth.from(datoTom))
        }
    }
}

sealed class EtterbetalingException {
    class EtterbetalingManglerDato : UgyldigForespoerselException(
        code = "MANGLER_FRA_ELLER_TIL_DATO",
        detail = "Etterbetaling må ha en fra-dato og en til-dato",
    )

    class EtterbetalingFomErEtterTom(fom: YearMonth, tom: YearMonth) : UgyldigForespoerselException(
        code = "FRA_DATO_ETTER_TIL_DATO",
        detail = "Fra-dato ($fom) kan ikke være etter til-dato ($tom).",
    )

    class EtterbetalingTomErFramITid(tom: YearMonth) : UgyldigForespoerselException(
        code = "TIL_DATO_FRAM_I_TID",
        detail = "Til-dato ($tom) er fram i tid.",
    )

    class EtterbetalingFraDatoErFoerVirk(fom: YearMonth, virkningstidspunkt: YearMonth) : UgyldigForespoerselException(
        code = "FRA_DATO_FOER_VIRK",
        detail = "Fra-dato ($fom) er før virkningstidspunkt ($virkningstidspunkt)",
    )
}
