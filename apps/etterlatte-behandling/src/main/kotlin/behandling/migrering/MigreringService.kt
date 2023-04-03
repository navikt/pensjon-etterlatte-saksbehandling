package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.inTransaction
import java.util.*

class MigreringService {
    fun migrer(): UUID {
        val behandlingId = inTransaction {
            UUID.randomUUID()
        }
        return behandlingId
    }
}