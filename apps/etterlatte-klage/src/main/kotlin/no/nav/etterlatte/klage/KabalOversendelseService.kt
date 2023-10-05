package no.nav.etterlatte.klage

import no.nav.etterlatte.klage.modell.KabalOversendelse
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Klage

interface KabalOversendelseService {
    suspend fun sendTilKabal(
        klage: Klage,
        ekstraDataInnstilling: EkstradataInnstilling,
    )
}

class KabalOversendelseServiceImpl(private val kabalKlient: KabalKlient) : KabalOversendelseService {
    override suspend fun sendTilKabal(
        klage: Klage,
        ekstraDataInnstilling: EkstradataInnstilling,
    ) {
        val oversendelse = KabalOversendelse.fra(klage = klage, ekstraData = ekstraDataInnstilling)
        kabalKlient.sendTilKabal(oversendelse)
    }
}
