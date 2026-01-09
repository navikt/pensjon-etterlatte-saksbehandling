package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Etterbetaling(
    val behandlingId: UUID,
    val fom: YearMonth,
    val tom: YearMonth,
    val kilde: Grunnlagsopplysning.Saksbehandler,
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
            behandlingId: UUID,
            datoFom: LocalDate?,
            datoTom: LocalDate?,
            kilde: Grunnlagsopplysning.Saksbehandler,
        ): Etterbetaling {
            if (datoFom == null || datoTom == null) {
                throw EtterbetalingException.EtterbetalingManglerDato()
            }
            return Etterbetaling(
                behandlingId = behandlingId,
                fom = YearMonth.from(datoFom),
                tom = YearMonth.from(datoTom),
                kilde = kilde,
            )
        }
    }

    fun toEtterbetalingDTO(): EtterbetalingDTO = EtterbetalingDTO(fom.atDay(1), tom.atEndOfMonth())
}

sealed class EtterbetalingException {
    class EtterbetalingManglerDato :
        UgyldigForespoerselException(
            code = "MANGLER_FRA_ELLER_TIL_DATO",
            detail = "Etterbetaling må ha en fra-dato og en til-dato",
        )

    class EtterbetalingFomErEtterTom(
        fom: YearMonth,
        tom: YearMonth,
    ) : UgyldigForespoerselException(
            code = "FRA_DATO_ETTER_TIL_DATO",
            detail = "Fra-dato ($fom) kan ikke være etter til-dato ($tom).",
        )

    class EtterbetalingTomErFramITid(
        tom: YearMonth,
    ) : UgyldigForespoerselException(
            code = "TIL_DATO_FRAM_I_TID",
            detail = "Til-dato ($tom) er fram i tid.",
        )

    class EtterbetalingFraDatoErFoerVirk(
        fom: YearMonth,
        virkningstidspunkt: YearMonth,
    ) : UgyldigForespoerselException(
            code = "FRA_DATO_FOER_VIRK",
            detail = "Fra-dato ($fom) er før virkningstidspunkt ($virkningstidspunkt)",
        )
}
