package no.nav.etterlatte.behandling.jobs.brevjobber

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class Arbeidsjobb(
    val id: UUID,
    val sakId: SakId,
    val type: JobbType,
    val status: ArbeidStatus,
    val resultat: String? = null,
    val merknad: String? = null,
    val opprettet: Tidspunkt,
    val sistEndret: Tidspunkt,
)

fun lagNyArbeidsJobb(
    sakId: SakId,
    type: JobbType,
    merknad: String?,
): Arbeidsjobb =
    Arbeidsjobb(
        id = UUID.randomUUID(),
        sakId = sakId,
        type = type,
        status = ArbeidStatus.NY,
        merknad = merknad,
        opprettet = Tidspunkt.now(),
        sistEndret = Tidspunkt.now(),
    )

enum class ArbeidStatus {
    NY,
    PAAGAAENDE,
    STANSET,
    FERDIG,
    FEILET,
}
