package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import java.util.UUID

internal class EtterbetalingService(private val dao: EtterbetalingDao) {
    fun lagreEtterbetaling(etterbetaling: Etterbetaling) = dao.lagreEtterbetaling(etterbetaling)

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? = dao.hentEtterbetaling(behandlingId)

    fun slettEtterbetaling(behandlingId: UUID) = dao.slettEtterbetaling(behandlingId)
}
