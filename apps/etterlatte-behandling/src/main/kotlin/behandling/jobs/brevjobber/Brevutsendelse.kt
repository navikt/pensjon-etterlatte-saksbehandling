package no.nav.etterlatte.behandling.jobs.brevjobber

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class Brevutsendelse(
    val id: UUID,
    val sakId: SakId,
    val type: BrevutsendelseType,
    val status: BrevutsendelseStatus,
    val resultat: String? = null,
    val merknad: String? = null,
    val opprettet: Tidspunkt,
    val sistEndret: Tidspunkt,
) {
    fun oppdaterStatus(status: BrevutsendelseStatus): Brevutsendelse = this.copy(status = status)
}

fun opprettNyBrevutsendelse(
    sakId: SakId,
    type: BrevutsendelseType,
    merknad: String?,
): Brevutsendelse =
    Brevutsendelse(
        id = UUID.randomUUID(),
        sakId = sakId,
        type = type,
        status = BrevutsendelseStatus.NY,
        merknad = merknad,
        opprettet = Tidspunkt.now(),
        sistEndret = Tidspunkt.now(),
    )

enum class BrevutsendelseStatus {
    NY,
    PAAGAAENDE,
    STANSET,
    FERDIG,
    FEILET,
}

enum class BrevutsendelseType(
    val beskrivelse: String,
    val kategori: BrevutsendelseKategori,
    val sakType: SakType?,
) {
    TREKKPLIKT_2025("Trekkplikt 2025, du taper penger på dette", BrevutsendelseKategori.TREKKPLIKT, SakType.BARNEPENSJON),
}

enum class BrevutsendelseKategori {
    TREKKPLIKT,
}
