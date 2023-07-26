package no.nav.etterlatte.behandling.migrering

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import org.slf4j.LoggerFactory
import java.sql.Connection

class MigreringRepository(private val connection: () -> Connection) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreKoplingTilPesyssaka(pesysSakId: PesysId, sakId: Long) = inTransaction {
        connection().prepareStatement("INSERT INTO pesyskopling(pesys_id,sak_id) VALUES(?, ?)")
            .also {
                it.setString(1, pesysSakId.id)
                it.setLong(2, sakId)
                it.executeUpdate()
            }
        logger.info("Oppretta kopling fra sak $sakId til pesyssak $pesysSakId")
    }
}