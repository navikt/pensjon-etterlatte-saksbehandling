package no.nav.etterlatte.behandling.doedshendelse

import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

class DoedshendelseReminder(
    val id: UUID = UUID.randomUUID(),
    val beroertFnr: String,
    val relasjon: Relasjon,
    val endret: Tidspunkt,
    val sakId: SakId? = null,
)
