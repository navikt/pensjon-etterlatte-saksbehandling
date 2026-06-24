package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg

enum class FeilutbetalingType {
    FEILUTBETALING_UTEN_VARSEL,
    FEILUTBETALING_4RG_UTEN_VARSEL,
    FEILUTBETALING_MED_VARSEL,
    INGEN_FEILUTBETALING,
    ;

    companion object {
        fun fromFeilutbetalingValg(feilutbetalingValg: FeilutbetalingValg) =
            when (feilutbetalingValg) {
                FeilutbetalingValg.NEI -> INGEN_FEILUTBETALING
                FeilutbetalingValg.JA_INGEN_TK -> FEILUTBETALING_4RG_UTEN_VARSEL
                FeilutbetalingValg.JA_INGEN_VARSEL_MOTREGNES -> FEILUTBETALING_UTEN_VARSEL
                FeilutbetalingValg.JA_VARSEL -> FEILUTBETALING_MED_VARSEL
            }
    }
}
