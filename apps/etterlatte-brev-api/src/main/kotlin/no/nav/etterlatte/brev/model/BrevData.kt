package no.nav.etterlatte.brev.model

interface BrevData

@Deprecated(
    "Denne bør vi kun trenge for dataklassene under - ManueltBrevData og ManueltBrevMedTittelData." +
        "For andre brevmaler dekker den redigerbare delen dette behovet",
)
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
