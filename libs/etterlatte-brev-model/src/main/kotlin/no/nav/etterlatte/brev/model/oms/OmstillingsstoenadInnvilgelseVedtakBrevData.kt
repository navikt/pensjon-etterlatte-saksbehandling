package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Vedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

object OmstillingsstoenadInnvilgelseVedtakBrevData {
    data class Vedtak(
        val beregning: OmstillingsstoenadBeregning,
        val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
        val omsRettUtenTidsbegrensning: Boolean,
        val etterbetaling: no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling?,
        val harUtbetaling: Boolean,
        val bosattUtland: Boolean,
        val erSluttbehandling: Boolean,
        val tidligereFamiliepleier: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
    ) : BrevFastInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_INNVILGELSE"

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
            )

        override val brevKode: Brevkoder = Brevkoder.OMS_INNVILGELSE
    }

    data class VedtakInnhold(
        val virkningsdato: LocalDate,
        val utbetalingsbeloep: Kroner,
        val etterbetaling: Boolean,
        val avdoed: no.nav.etterlatte.brev.behandling.Avdoed?,
        val harUtbetaling: Boolean,
        val beregning: OmstillingsstoenadBeregning,
        val erSluttbehandling: Boolean = false,
        val tidligereFamiliepleier: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
    ) : BrevRedigerbarInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL"

        override val brevKode: Brevkoder = Brevkoder.OMS_INNVILGELSE
    }

    fun beregningsvedleggInnhold(): BrevVedleggRedigerbarNy =
        BrevVedleggRedigerbarNy(
            data = null,
            vedlegg = Vedlegg.OMS_BEREGNING,
            vedleggId = BrevVedleggKey.OMS_BEREGNING,
        )

    data class Avdoed(
        val fnr: Foedselsnummer,
        val navn: String,
        val doedsdato: LocalDate,
    )
}
