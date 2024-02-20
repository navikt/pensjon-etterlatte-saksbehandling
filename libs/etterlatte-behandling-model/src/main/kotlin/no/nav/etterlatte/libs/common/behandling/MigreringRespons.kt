package no.nav.etterlatte.libs.common.behandling

import java.util.UUID

data class MigreringRespons(val behandlingId: UUID, val sakId: Long, val oppgaveId: UUID)
