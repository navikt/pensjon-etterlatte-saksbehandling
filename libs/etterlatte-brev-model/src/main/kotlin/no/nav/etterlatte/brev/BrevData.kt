package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBrevInnholdDataNy
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.sak.Sak

/* Ikke utvid denne direkte,
utvid heller BrevDataFerdigstilling eller BrevDataRedigerbar,
avhengig av hva du lager
*/
interface BrevData

interface BrevDataFerdigstilling : BrevData {
    val innhold: List<Slate.Element>
}

interface BrevDataRedigerbar : BrevData

interface HarVedlegg : BrevData {
    val innhold: List<Slate.Element>
}

data class ManueltBrevData(
    val innhold: List<Slate.Element> = emptyList(),
) : BrevDataRedigerbar

data class ManueltBrevMedTittelData(
    override val innhold: List<Slate.Element>,
    val tittel: String? = null,
) : BrevDataFerdigstilling

// TODO

data class BrevDataFerdigstillingNy(
    override val innhold: List<Slate.Element>,
    val data: BrevInnholdData,
) : BrevDataFerdigstilling

data class BrevDataRedigerbarNy(
    val brevInnholdData: BrevInnholdData,
) : BrevData

data class BrevRequest(
    val spraak: Spraak, // TODO ?
    val sak: Sak,
    val innsender: Innsender?,
    val soeker: Soeker,
    val avdoede: List<Avdoed>,
    val verge: Verge?,
    val saksbehandlerIdent: String,
    val attestantIdent: String,
    val skalLagre: Boolean,
    val brevInnholdData: BrevInnholdData,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TilbakekrevingBrevInnholdDataNy::class, name = "TILBAKEKREVING"),
)
abstract class BrevInnholdData(
    open val type: String,
) {
    abstract val brevKode: Brevkoder
}
