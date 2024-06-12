package no.nav.etterlatte.behandling.bosattutland

import no.nav.etterlatte.behandling.utland.LandMedDokumenter
import java.util.UUID

class BosattUtlandService(
    val bosattUtlandDao: BosattUtlandDao,
) {
    fun lagreBosattUtland(bosattUtland: BosattUtland): BosattUtland {
        bosattUtlandDao.lagreBosattUtland(bosattUtland)
        return hentBosattUtland(bosattUtland.behandlingId)!!
    }

    fun hentBosattUtland(behandlingid: UUID): BosattUtland? = bosattUtlandDao.hentBosattUtland(behandlingid)
}

data class BosattUtland(
    val behandlingId: UUID,
    val rinanummer: String,
    val mottatteSeder: List<LandMedDokumenter>,
    val sendteSeder: List<LandMedDokumenter>,
)
