package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import java.time.LocalDate

data class InnvilgetBrevDataEnkel(
    val utbetalingsinfo: Utbetalingsinfo,
    val avdoed: Avdoed,
    val vedtaksdato: LocalDate,
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling) =
            InnvilgetBrevDataEnkel(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avdoed = behandling.personerISak.avdoed,
                vedtaksdato =
                    behandling.vedtak.vedtaksdato
                        ?: LocalDate.now(),
            )
    }
}

data class InnvilgetHovedmalBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avkortingsinfo: Avkortingsinfo? = null,
    val beregningsinfo: BeregningsinfoBP,
    val etterbetalingDTO: EtterbetalingDTO? = null,
    val innhold: List<Slate.Element>,
) : BrevData() {
    companion object {
        fun fra(
            behandling: Behandling,
            innhold: InnholdMedVedlegg,
        ): InnvilgetHovedmalBrevData =
            InnvilgetHovedmalBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avkortingsinfo = behandling.avkortingsinfo,
                beregningsinfo = tilBeregningsinfo(behandling, innhold),
                etterbetalingDTO = behandling.etterbetalingDTO,
                innhold = innhold.innhold(),
            )

        private fun tilBeregningsinfo(
            behandling: Behandling,
            innhold: InnholdMedVedlegg,
        ): BeregningsinfoBP {
            return BeregningsinfoBP(
                innhold = innhold.finnVedlegg(BrevVedleggKey.BP_BEREGNING_TRYGDETID),
                grunnbeloep = behandling.utbetalingsinfo!!.beregningsperioder.last().grunnbeloep, // TODO
                beregningsperioder = behandling.utbetalingsinfo.beregningsperioder,
                antallBarn = behandling.utbetalingsinfo.antallBarn,
                samlaTrygdetid = "",
                trygdetidsperioder = behandling.trygdetid ?: listOf(),
            )
        }
    }
}
