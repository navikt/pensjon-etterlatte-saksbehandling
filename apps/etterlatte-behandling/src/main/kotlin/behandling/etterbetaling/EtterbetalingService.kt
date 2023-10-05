package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import java.util.UUID

internal class EtterbetalingService(private val dao: EtterbetalingDao) {
    fun lagreEtterbetaling(etterbetaling: Etterbetaling) = dao.lagreEtterbetaling(etterbetaling)

    fun hentEtterbetaling(behandlingsId: UUID): Etterbetaling? = dao.hentEtterbetaling(behandlingsId)

    fun slettEtterbetaling(behandlingsId: UUID) = dao.slettEtterbetaling(behandlingsId)
}
