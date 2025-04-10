package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData
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

// TODO Når alt er over på ny løsning bør gamle klasser slettes og nye renames

data class BrevDataFerdigstillingNy(
    override val innhold: List<Slate.Element>,
    val data: BrevFastInnholdData,
) : BrevDataFerdigstilling

data class BrevRequest(
    val spraak: Spraak, // TODO ?
    val sak: Sak,
    val innsender: Innsender?,
    val soeker: Soeker,
    val avdoede: List<Avdoed>,
    val verge: Verge?,
    val saksbehandlerIdent: String,
    val attestantIdent: String?,
    val skalLagre: Boolean,
    val brevFastInnholdData: BrevFastInnholdData,
    val brevRedigerbarInnholdData: BrevRedigerbarInnholdData?,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EtteroppgjoerBrevData.Forhaandsvarsel::class, name = "OMS_EO_FORHAANDSVARSEL"),
)
abstract class BrevFastInnholdData : BrevData {
    abstract val brevKode: Brevkoder
    abstract val type: String
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EtteroppgjoerBrevData.ForhaandsvarselInnhold::class, name = "OMS_EO_FORHAANDSVARSEL_REDIGERBAR"),
)
abstract class BrevRedigerbarInnholdData : BrevDataRedigerbar {
    abstract val brevKode: Brevkoder
    abstract val type: String
}
