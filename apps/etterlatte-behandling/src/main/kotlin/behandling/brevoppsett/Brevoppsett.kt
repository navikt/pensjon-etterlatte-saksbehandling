package no.nav.etterlatte.behandling.brevoppsett

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
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
            throw BrevoppsettException.EtterbetalingFomErEtterTom(fom, tom)
        }
        if (tom > YearMonth.now()) {
            throw BrevoppsettException.EtterbetalingTomErFramITid(tom)
        }
    }

    companion object {
        fun fra(
            datoFom: LocalDate?,
            datoTom: LocalDate?,
        ): Etterbetaling {
            if (datoFom == null || datoTom == null) {
                throw BrevoppsettException.EtterbetalingManglerDato()
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

sealed class BrevoppsettException {
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

    class BehandlingKanIkkeEndres(behandling: Behandling) : IkkeTillattException(
        code = "KAN_IKKE_ENDRES",
        detail = "Behandling ${behandling.id} har status ${behandling.status} og kan ikke endres.",
    )

    class VirkningstidspunktIkkeSatt(behandling: Behandling) : UgyldigForespoerselException(
        code = "VIRKNINGSTIDSPUNKT_IKKE_SATT",
        detail = "Behandling ${behandling.id} har ikke satt virkningstidspunkt.",
    )
}
