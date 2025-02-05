package no.nav.etterlatte.brev

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
    val brevInnholdData: BrevInnholdData,
) : BrevDataFerdigstilling

data class BrevDataRedigerbarNy(
    val brevInnholdData: BrevInnholdData,
) : BrevData

interface BrevInnholdData
