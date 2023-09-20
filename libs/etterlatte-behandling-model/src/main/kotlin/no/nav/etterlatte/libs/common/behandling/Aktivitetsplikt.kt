package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class AktivitetspliktOppfolging(
    val behandlingId: UUID,
    val aktivitet: String,
    val opprettet: Tidspunkt,
    val opprettetAv: String,
)

data class OpprettAktivitetspliktOppfolging(
    val aktivitet: String,
)
