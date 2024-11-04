package no.nav.etterlatte.brev.notat

import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.sak.SakId

typealias NotatID = Long

data class NyttNotat(
    val sakId: SakId,
    val referanse: String? = null,
    val tittel: String,
    val mal: NotatMal,
    val payload: Slate,
)

data class Notat(
    val id: NotatID,
    val sakId: SakId,
    val journalpostId: String? = null,
    val referanse: String? = null,
    val tittel: String,
    val opprettet: Tidspunkt,
) {
    fun kanRedigeres() = journalpostId.isNullOrBlank()
}

enum class NotatMal(
    val navn: String,
    val sti: String = "notat",
) {
    TOM_MAL("tom_mal"),
    NORDISK_VEDLEGG("nordisk_vedlegg"),
    MANUELL_SAMORDNING("manuell_samordning"),
    KLAGE_OVERSENDELSE_BLANKETT("klage_oversendelse_blankett"),
}
