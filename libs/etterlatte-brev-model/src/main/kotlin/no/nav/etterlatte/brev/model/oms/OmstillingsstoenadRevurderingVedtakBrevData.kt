package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import java.time.LocalDate

object OmstillingsstoenadRevurderingVedtakBrevData {
    data class Vedtak(
        val erEndret: Boolean,
        val erOmgjoering: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
        val beregning: OmstillingsstoenadBeregning,
        val omsRettUtenTidsbegrensning: Boolean,
        val feilutbetaling: FeilutbetalingType,
        val bosattUtland: Boolean,
        val erInnvilgelsesaar: Boolean,
        val tidligereFamiliepleier: Boolean,
        val innholdForhaandsvarsel: List<Slate.Element> = emptyList(),
    ) : BrevFastInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_REVURDERING"

        init {
            if (erOmgjoering && datoVedtakOmgjoering == null) {
                throw InternfeilException(
                    "Kunne ikke lage revurderingsbrevet for omstillingsstønad siden vi ikke" +
                        " fikk dato vedtak for omgjøring, i en revurdering som er omgjøring.",
                )
            }
        }

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                beregning =
                    beregning.copy(
                        innhold =
                            innhold()
                                .singleOrNull { it.key == BrevVedleggKey.OMS_BEREGNING }
                                ?.let { it.payload!!.elements }
                                ?: emptyList(),
                    ),
                innholdForhaandsvarsel =
                    innhold()
                        .takeIf { feilutbetaling == FeilutbetalingType.FEILUTBETALING_MED_VARSEL }
                        ?.single { it.key == BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING }
                        ?.let { it.payload!!.elements }
                        ?: emptyList(),
            )

        override val brevKode: Brevkoder = Brevkoder.OMS_INNVILGELSE
    }
}
