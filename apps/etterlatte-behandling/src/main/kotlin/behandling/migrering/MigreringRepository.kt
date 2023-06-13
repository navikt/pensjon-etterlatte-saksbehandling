package no.nav.etterlatte.behandling.migrering

import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class MigreringRepository(private val dataSource: DataSource) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreKoplingTilPesyssaka(pesysSakId: PesysId, sakId: Long) = dataSource.opprett(
        "INSERT INTO pesyskopling(pesys_id,sak_id) VALUES(:pesysSakId, :sakId)",
        mapOf("pesysSakId" to pesysSakId.id, "sakId" to sakId),
        "Oppretta kopling fra sak $sakId til pesyssak $pesysSakId",
        logger
    )
}