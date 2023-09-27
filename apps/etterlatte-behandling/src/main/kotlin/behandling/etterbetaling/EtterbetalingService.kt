package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Etterbetalingmodell
import java.util.UUID

internal class EtterbetalingService(private val dao: EtterbetalingDao) {
    fun lagreEtterbetaling(etterbetaling: Etterbetalingmodell) {
        inTransaction {
            dao.lagreEtterbetaling(etterbetaling)
        }
    }

    fun hentEtterbetaling(behandlingsId: UUID): Etterbetalingmodell? =
        inTransaction {
            dao.hentEtterbetaling(behandlingsId)
        }
}
