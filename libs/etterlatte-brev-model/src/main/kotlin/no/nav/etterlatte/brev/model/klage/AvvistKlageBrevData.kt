package no.nav.etterlatte.brev.model.klage

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate

@JsonTypeName("AVVIST_KLAGE")
data class AvvistKlageBrevInnholdDataNy(
    val data: AvvistKlageBrevDataInnholdData,
) : BrevFastInnholdData() {
    override val type: String = "AVVIST_KLAGE"

    override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData = this

    override val brevKode: Brevkoder = Brevkoder.AVVIST_KLAGE
}

data class AvvistKlageBrevDataInnholdData(
    val sakType: SakType,
    val bosattUtland: Boolean,
    val klageDato: LocalDate,
    val datoForVedtaketKlagenGjelder: LocalDate?,
)

@JsonTypeName("AVVIST_KLAGE_UTFALL")
data class AvvistKlageBrevRedigerbarInnholdData(
    val sakType: SakType,
    val bosattUtland: Boolean,
    val klageDato: LocalDate,
    val datoForVedtaketKlagenGjelder: LocalDate?,
) : BrevRedigerbarInnholdData() {
    override val type: String = "AVVIST_KLAGE_UTFALL"

    override val brevKode: Brevkoder = Brevkoder.AVVIST_KLAGE
}
