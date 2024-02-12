package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg

fun toFeilutbetalingType(feilutbetalingValg: FeilutbetalingValg) =
    when (feilutbetalingValg) {
        FeilutbetalingValg.NEI -> FeilutbetalingType.INGEN_FEILUTBETALING
        FeilutbetalingValg.JA_INGEN_TK -> FeilutbetalingType.FEILUTBETALING_UTEN_VARSEL
        FeilutbetalingValg.JA_VARSEL -> FeilutbetalingType.FEILUTBETALING_MED_VARSEL
    }

fun vedleggHvisFeilutbetaling(
    feilutbetaling: FeilutbetalingType,
    innholdMedVedlegg: InnholdMedVedlegg,
) = if (feilutbetaling == FeilutbetalingType.FEILUTBETALING_MED_VARSEL) {
    innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING)
} else {
    emptyList()
}
