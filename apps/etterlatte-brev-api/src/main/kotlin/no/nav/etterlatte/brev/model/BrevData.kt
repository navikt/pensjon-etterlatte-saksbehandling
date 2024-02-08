package no.nav.etterlatte.brev.model

abstract class BrevData

data class ManueltBrevData(val innhold: List<Slate.Element> = emptyList()) : BrevData()

data class ManueltBrevMedTittelData(val innhold: List<Slate.Element>, val tittel: String? = null) : BrevData()
