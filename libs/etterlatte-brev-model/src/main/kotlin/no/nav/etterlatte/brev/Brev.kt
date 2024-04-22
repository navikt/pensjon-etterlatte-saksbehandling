package no.nav.etterlatte.brev.model

typealias BrevID = Long

enum class Status {
    OPPRETTET,
    OPPDATERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    SLETTET,
}

enum class BrevProsessType {
    MANUELL,
    REDIGERBAR,
    AUTOMATISK,
    OPPLASTET_PDF,
}
