package no.nav.etterlatte.brev.model.klage

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import java.time.LocalDate

data class AvvistKlageFerdigData(
    override val innhold: List<Slate.Element>,
    val data: AvvistKlageInnholdBrevData,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            klage: Klage?,
            utlandstilknytningType: UtlandstilknytningType?,
        ): AvvistKlageFerdigData =
            AvvistKlageFerdigData(
                innhold = innholdMedVedlegg.innhold(),
                data = AvvistKlageInnholdBrevData.fra(klage, utlandstilknytningType),
            )
    }
}

data class AvvistKlageInnholdBrevData(
    val sakType: SakType,
    val klageDato: LocalDate,
    val datoForVedtaketKlagenGjelder: LocalDate?,
    val bosattUtland: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            muligKlage: Klage?,
            utlandstilknytningType: UtlandstilknytningType?,
        ): AvvistKlageInnholdBrevData {
            val klage = muligKlage ?: throw IllegalArgumentException("Vedtak mangler klage")
            return AvvistKlageInnholdBrevData(
                sakType = klage.sak.sakType,
                klageDato = klage.innkommendeDokument?.mottattDato ?: klage.opprettet.toLocalDate(),
                datoForVedtaketKlagenGjelder =
                    klage.formkrav
                        ?.formkrav
                        ?.vedtaketKlagenGjelder
                        ?.datoAttestert
                        ?.toLocalDate(),
                bosattUtland = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
            )
        }
    }
}
