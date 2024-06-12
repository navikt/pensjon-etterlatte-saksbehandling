package no.nav.etterlatte.brev.notat

import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

typealias NotatID = Long

data class NyttNotat(
    val sakId: Long,
    val tittel: String,
    val mal: NotatMal,
    val payload: Slate,
)

data class Notat(
    val id: NotatID,
    val sakId: Long,
    val journalpostId: String? = null,
    val tittel: String,
    val opprettet: Tidspunkt,
) {
    fun kanRedigeres() = journalpostId.isNullOrBlank()
}

enum class NotatMal(
    val navn: String,
) {
    TOM_MAL("tom_mal"),
    NORDISK_VEDLEGG("nordisk_vedlegg"),
}
