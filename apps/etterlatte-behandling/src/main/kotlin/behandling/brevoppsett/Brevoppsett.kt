package no.nav.etterlatte.behandling.brevoppsett

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Brevoppsett(
    val behandlingId: UUID,
    val etterbetaling: Etterbetaling?,
    val brevtype: Brevtype,
    val aldersgruppe: Aldersgruppe?,
    val kilde: Grunnlagsopplysning.Kilde,
)

data class Etterbetaling(
    val fom: YearMonth,
    val tom: YearMonth,
) {
    init {
        if (fom > tom) {
            throw EtterbetalingException.FomErEtterTom(fom, tom)
        }
        if (tom > YearMonth.now()) {
            throw EtterbetalingException.TomErFramITid(tom)
        }
    }

    companion object {
        fun fra(
            datoFom: LocalDate?,
            datoTom: LocalDate?,
        ): Etterbetaling {
            if (datoFom == null || datoTom == null) {
                throw EtterbetalingException.ManglerDato()
            }
            return Etterbetaling(YearMonth.from(datoFom), YearMonth.from(datoTom))
        }
    }
}

enum class Brevtype {
    NASJONAL,
    UTLAND,
}

enum class Aldersgruppe {
    OVER_18,
    UNDER_18,
}

sealed class EtterbetalingException {
    class ManglerDato : UgyldigForespoerselException(
        code = "MANGLER_FRA_ELLER_TIL_DATO",
        detail = "Etterbetaling må ha en fra-dato og en til-dato",
    )

    class FomErEtterTom(fom: YearMonth, tom: YearMonth) : UgyldigForespoerselException(
        code = "FRA_DATO_ETTER_TIL_DATO",
        detail = "Fra-dato ($fom) kan ikke være etter til-dato ($tom).",
    )

    class TomErFramITid(tom: YearMonth) : UgyldigForespoerselException(
        code = "TIL_DATO_FRAM_I_TID",
        detail = "Til-dato ($tom) er fram i tid.",
    )

    class FraDatoErFoerVirk(fom: YearMonth, virkningstidspunkt: YearMonth) : UgyldigForespoerselException(
        code = "FRA_DATO_FOER_VIRK",
        detail = "Fra-dato ($fom) er før virkningstidspunkt ($virkningstidspunkt)",
    )
}
