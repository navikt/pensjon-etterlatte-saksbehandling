package no.nav.etterlatte.brev.model.klage

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate

data class AvvistKlageFerdigData(
    override val innhold: List<Slate.Element>,
    val data: AvvistKlageInnholdBrevData,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            innholdMedVedlegg: InnholdMedVedlegg,
        ): AvvistKlageFerdigData =
            AvvistKlageFerdigData(
                innhold = innholdMedVedlegg.innhold(),
                data = AvvistKlageInnholdBrevData.fra(generellBrevData),
            )
    }
}

data class AvvistKlageInnholdBrevData(
    val sakType: SakType,
    val klageDato: LocalDate,
    val datoForVedtaketKlagenGjelder: LocalDate?,
) : BrevDataRedigerbar {
    companion object {
        fun fra(generellBrevData: GenerellBrevData): AvvistKlageInnholdBrevData {
            val klage = generellBrevData.forenkletVedtak?.klage ?: throw IllegalArgumentException("Vedtak mangler klage")
            return AvvistKlageInnholdBrevData(
                sakType = klage.sak.sakType,
                klageDato = klage.innkommendeDokument?.mottattDato ?: klage.opprettet.toLocalDate(),
                datoForVedtaketKlagenGjelder =
                    klage.formkrav
                        ?.formkrav
                        ?.vedtaketKlagenGjelder
                        ?.datoAttestert
                        ?.toLocalDate(),
            )
        }
    }
}
