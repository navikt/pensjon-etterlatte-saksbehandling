package no.nav.etterlatte.brev.notat

import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

typealias NotatID = Long

data class NyttNotat(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val referanse: String? = null,
    val tittel: String,
    val mal: NotatMal,
    val payload: Slate,
)

data class Notat(
    val id: NotatID,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val journalpostId: String? = null,
    val referanse: String? = null,
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
    MANUELL_SAMORDNING("manuell_samordning"),
    KLAGE_OVERSENDELSE_BLANKETT("klage_oversendelse_blankett"),
}
