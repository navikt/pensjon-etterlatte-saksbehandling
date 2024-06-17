package no.nav.etterlatte.klage

import no.nav.etterlatte.klage.modell.KabalOversendelse
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Klage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface KabalOversendelseService {
    suspend fun sendTilKabal(
        klage: Klage,
        ekstraDataInnstilling: EkstradataInnstilling,
    )
}

class KabalOversendelseServiceImpl(
    private val kabalKlient: KabalKlient,
) : KabalOversendelseService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun sendTilKabal(
        klage: Klage,
        ekstraDataInnstilling: EkstradataInnstilling,
    ) {
        logger.info("Mottok en klage med id=${klage.id}, oversender den til Kabal")
        val oversendelse = KabalOversendelse.fra(klage = klage, ekstraData = ekstraDataInnstilling)
        kabalKlient.sendTilKabal(oversendelse)
        logger.info("Klage med id=${klage.id} oversendt til Kabal uten feil")
    }
}
