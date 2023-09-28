package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import java.util.UUID

internal class EtterbetalingService(private val dao: EtterbetalingDao) {
    fun lagreEtterbetaling(etterbetaling: Etterbetaling) {
        inTransaction {
            dao.lagreEtterbetaling(etterbetaling)
        }
    }

    fun hentEtterbetaling(behandlingsId: UUID): Etterbetaling? =
        inTransaction {
            dao.hentEtterbetaling(behandlingsId)
        }
}
