package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

abstract class EndringBrevData : BrevData()

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): SoeskenjusteringRevurderingBrevdata {
            if (behandling.revurderingsaarsak != RevurderingAarsak.SOESKENJUSTERING) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for søskenjustering når " +
                        "revurderingsårsak er ${behandling.revurderingsaarsak}"
                )
            }
            if (behandling.revurderingInfo !is RevurderingInfo.Soeskenjustering) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for søskenjustering når " +
                        "revurderingsinfo ikke er Søskenjustering"
                )
            }

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = requireNotNull(behandling.utbetalingsinfo) {
                    "Kan ikke opprette et revurderingsbrev for søksenjustering uten utbetalingsinfo"
                },
                grunnForJustering = behandling.revurderingInfo.grunnForSoeskenjustering
            )
        }
    }
}

data class FengselsoppholdBrevdata(
    val virkningsdato: LocalDate,
    val fraDato: LocalDate,
    val tilDato: LocalDate
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): FengselsoppholdBrevdata {
            if (behandling.revurderingsaarsak != RevurderingAarsak.FENGSELSOPPHOLD) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for fengselsopphold når " +
                        "revurderingsårsak er ${behandling.revurderingsaarsak}"
                )
            }
            if (behandling.revurderingInfo !is RevurderingInfo.Fengselsopphold) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for fengselsopphold når " +
                        "revurderingsinfo ikke er fengselsopphold"
                )
            }
            return FengselsoppholdBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                fraDato = behandling.revurderingInfo.fraDato,
                tilDato = behandling.revurderingInfo.tilDato
            )
        }
    }
}

data class UtAvFengselBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val erEtterbetalingMerEnnTreeMaaneder: Boolean,
    val virkningsdato: LocalDate,
    val fraDato: LocalDate,
    val tilDato: LocalDate
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): UtAvFengselBrevdata {
            if (behandling.revurderingsaarsak != RevurderingAarsak.UT_AV_FENGSEL) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for ut av fengselsopphold når " +
                        "revurderingsårsak er ${behandling.revurderingsaarsak}"
                )
            }
            if (behandling.revurderingInfo !is RevurderingInfo.UtAvFengsel) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for ut av fengselsopphold når " +
                        "revurderingsinfo ikke er ut av fengselsopphold"
                )
            }
            return UtAvFengselBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                erEtterbetalingMerEnnTreeMaaneder = behandling.revurderingInfo.erEtterbetalingMerEnnTreeMaaneder,
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                fraDato = behandling.revurderingInfo.fraDato,
                tilDato = behandling.revurderingInfo.tilDato
            )
        }
    }
}