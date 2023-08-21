package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.AvslagBrevData.valider
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

abstract class EndringBrevData : BrevData()

data class EtterbetalingDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val beregningsperioder: List<Etterbetalingsperiode>
)

data class Etterbetalingsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner
)
data class EndringHovedmalBrevData(
    val erEndret: Boolean,
    val etterbetaling: EtterbetalingDTO,
    val utbetalingsinfo: Utbetalingsinfo,
    val innhold: List<Slate.Element>
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling, innhold: List<Slate.Element>): BrevData = EndringHovedmalBrevData(
            erEndret = true, // TODO når resten av fengselsopphold implementerast
            etterbetaling = EtterbetalingDTO(
                fraDato = LocalDate.now(), // TODO når resten av fengselsopphold implementerast
                tilDato = LocalDate.now(), // TODO når resten av fengselsopphold implementerast
                beregningsperioder = behandling.utbetalingsinfo.beregningsperioder.map {
                    Etterbetalingsperiode(
                        datoFOM = it.datoFOM,
                        datoTOM = it.datoTOM,
                        grunnbeloep = it.grunnbeloep,
                        stoenadFoerReduksjon = it.utbetaltBeloep, // TODO når resten av fengselsopphold implementerast
                        utbetaltBeloep = it.utbetaltBeloep
                    )
                }
            ),
            utbetalingsinfo = behandling.utbetalingsinfo,
            innhold = innhold
        )
    }
}

data class SoeskenjusteringRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val grunnForJustering: BarnepensjonSoeskenjusteringGrunn
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): SoeskenjusteringRevurderingBrevdata {
            val revurderingsinfo = valider<RevurderingInfo.Soeskenjustering>(
                behandling,
                RevurderingAarsak.SOESKENJUSTERING
            )

            return SoeskenjusteringRevurderingBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo,
                grunnForJustering = revurderingsinfo.grunnForSoeskenjustering
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
            val revurderingInfo = valider<RevurderingInfo.Fengselsopphold>(
                behandling,
                RevurderingAarsak.FENGSELSOPPHOLD
            )
            return FengselsoppholdBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                fraDato = revurderingInfo.fraDato,
                tilDato = revurderingInfo.tilDato
            )
        }
    }
}

data class UtAvFengselBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val erEtterbetalingMerEnnTreeMaaneder: Boolean,
    val virkningsdato: LocalDate
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): UtAvFengselBrevdata {
            val revurderingInfo = valider<RevurderingInfo.UtAvFengsel>(
                behandling,
                RevurderingAarsak.UT_AV_FENGSEL
            )
            return UtAvFengselBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo,
                erEtterbetalingMerEnnTreeMaaneder = revurderingInfo.erEtterbetalingMerEnnTreeMaaneder,
                virkningsdato = behandling.virkningsdato!!.atDay(1)
            )
        }
    }
}

data class YrkesskadeRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val stoenadHarOekt: Boolean,
    val yrkesskadeErDokumentert: Boolean,
    val virkningsdato: LocalDate
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): YrkesskadeRevurderingBrevdata {
            val stonadHarOekt = behandling.utbetalingsinfo.beloep.value >
                behandling.forrigeUtbetalingsinfo!!.beloep.value
            return YrkesskadeRevurderingBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo,
                stoenadHarOekt = stonadHarOekt,
                yrkesskadeErDokumentert = true, // TODO
                virkningsdato = behandling.virkningsdato!!.atDay(1)
            )
        }
    }
}

data class InstitusjonsoppholdRevurderingBrevdata(
    val utbetalingsinfo: Utbetalingsinfo,
    val erEtterbetalingMerEnnTreMaaneder: Boolean,
    val virkningsdato: LocalDate,
    val prosent: Int?,
    val innlagtdato: LocalDate?,
    val utskrevetdato: LocalDate?
) : EndringBrevData() {

    companion object {
        fun fra(behandling: Behandling): InstitusjonsoppholdRevurderingBrevdata {
            val revurderingInfo = valider<RevurderingInfo.Institusjonsopphold>(
                behandling,
                RevurderingAarsak.INSTITUSJONSOPPHOLD
            )
            return InstitusjonsoppholdRevurderingBrevdata(
                utbetalingsinfo = behandling.utbetalingsinfo,
                erEtterbetalingMerEnnTreMaaneder = revurderingInfo.erEtterbetalingMerEnnTreMaaneder,
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                prosent = revurderingInfo.prosent,
                innlagtdato = revurderingInfo.innlagtdato,
                utskrevetdato = revurderingInfo.utskrevetdato
            )
        }
    }
}