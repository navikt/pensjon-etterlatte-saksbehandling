package no.nav.etterlatte.brev.model

interface BrevData

interface BrevdataMedInnhold : BrevData {
    val innhold: List<Slate.Element>
}

interface BrevDataFerdigstilling : BrevdataMedInnhold

interface BrevDataRedigerbar : BrevData

data class ManueltBrevData(
    override val innhold: List<Slate.Element> = emptyList(),
) : BrevdataMedInnhold,
    BrevDataRedigerbar

data class ManueltBrevMedTittelData(
    override val innhold: List<Slate.Element>,
    val tittel: String? = null,
) : BrevDataFerdigstilling
