package no.nav.etterlatte.migrering.start

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_FLERE_KEY
import java.util.UUID
import javax.sql.DataSource

class StartMigrering(val repository: StartMigreringRepository, val rapidsConnection: RapidsConnection) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startMigrering() {
        logger.info("Starter migrering for sakene som ligger i databasetabellen")
        val sakerTilMigrering = repository.hentSakerTilMigrering().also { logger.info("Migrerer ${it.size} saker") }
        repository.settSakerMigrert(sakerTilMigrering)
        rapidsConnection.publish(message = lagMelding(sakerTilMigrering), key = UUID.randomUUID().toString())
    }

    private fun lagMelding(sakerTilMigrering: List<Long>) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to Migreringshendelser.START_MIGRERING,
                SAK_ID_FLERE_KEY to sakerTilMigrering,
                LOPENDE_JANUAR_2024_KEY to true,
                MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.MED_PAUSE,
            ),
        ).toJson()
}

class StartMigreringRepository(private val dataSource: DataSource) : Transactions<StartMigreringRepository> {
    override fun <R> inTransaction(block: StartMigreringRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun hentSakerTilMigrering(tx: TransactionalSession? = null) =
        tx.session {
            hentListe(
                queryString =
                    "SELECT ${Databasetabell.SAKID} FROM ${Databasetabell.TABELLNAVN} " +
                        "WHERE ${Databasetabell.HAANDTERT} = FALSE",
            ) {
                it.long(Databasetabell.SAKID)
            }
        }

    fun settSakerMigrert(
        saker: List<Long>,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query =
                "UPDATE ${Databasetabell.TABELLNAVN} SET ${Databasetabell.HAANDTERT} = true WHERE " +
                    "${Databasetabell.SAKID} = ANY(:saker)",
            params =
                mapOf(
                    "saker" to createArrayOf("bigint", saker),
                ),
            loggtekst = "Markerte $saker som påstarta for migrering",
        )
    }

    internal object Databasetabell {
        const val TABELLNAVN = "saker_til_migrering"
        const val HAANDTERT = "haandtert"
        const val SAKID = "sakId"
    }
}
