package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import java.util.*

class AktivitetspliktService(private val aktivitetspliktDao: AktivitetspliktDao) {

    fun hentAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? {
        return inTransaction {
            aktivitetspliktDao.finnSenesteAktivitetspliktOppfolging(behandlingId)
        }
    }

    fun lagreAktivitetspliktOppfolging(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String
    ): AktivitetspliktOppfolging {
        inTransaction {
            aktivitetspliktDao.lagre(behandlingId, nyOppfolging, navIdent)
        }

        return hentAktivitetspliktOppfolging(behandlingId)!!
    }
}