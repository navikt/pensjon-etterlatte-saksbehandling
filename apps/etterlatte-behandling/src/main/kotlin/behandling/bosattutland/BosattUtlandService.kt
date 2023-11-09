package no.nav.etterlatte.behandling.bosattutland

import behandling.utland.LandMedDokumenter
import java.util.UUID

class BosattUtlandService(val bosattUtlandDao: BosattUtlandDao) {
    fun lagreBosattUtland(bosattUtland: BosattUtland): BosattUtland {
        return bosattUtlandDao.lagreBosattUtland(bosattUtland)
    }

    fun hentBosattUtland(behandlingid: UUID): BosattUtland? {
        return bosattUtlandDao.hentBosattUtland(behandlingid)
    }
}

data class BosattUtland(
    val behandlingid: UUID,
    val rinanummer: String,
    val mottatteSeder: List<LandMedDokumenter>,
    val sendteSeder: List<LandMedDokumenter>,
)
