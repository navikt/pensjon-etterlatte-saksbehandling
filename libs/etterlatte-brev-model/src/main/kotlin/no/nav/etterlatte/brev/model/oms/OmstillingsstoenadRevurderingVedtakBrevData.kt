package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import java.time.LocalDate

object OmstillingsstoenadRevurderingVedtakBrevData {
    data class Vedtak(
        val innholdForhaandsvarsel: List<Slate.Element> = emptyList(),
        val erEndret: Boolean,
        val erOmgjoering: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
        val beregning: OmstillingsstoenadBeregning,
        val omsRettUtenTidsbegrensning: Boolean,
        val feilutbetaling: FeilutbetalingType,
        val bosattUtland: Boolean,
        val erInnvilgelsesaar: Any,
        val tidligereFamiliepleier: Boolean,
    ) : BrevFastInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_REVURDERING" // TODO

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                beregning =
                    beregning.copy(
                        innhold =
                            innhold()
                                .single { it.key == BrevVedleggKey.OMS_BEREGNING }
                                .payload!!
                                .elements,
                    ),
                innholdForhaandsvarsel =
                    if (feilutbetaling == FeilutbetalingType.FEILUTBETALING_MED_VARSEL) {
                        innhold()
                            .single { it.key == BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING }
                            .payload!!
                            .elements
                    } else {
                        emptyList()
                    },
            )

        override val brevKode: Brevkoder = Brevkoder.OMS_INNVILGELSE
    }
}

enum class FeilutbetalingType {
    // TODO dra inn isf duplikat?
    FEILUTBETALING_UTEN_VARSEL,
    FEILUTBETALING_4RG_UTEN_VARSEL,
    FEILUTBETALING_MED_VARSEL,
    INGEN_FEILUTBETALING,
}
