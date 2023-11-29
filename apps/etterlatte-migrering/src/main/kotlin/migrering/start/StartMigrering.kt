package no.nav.etterlatte.migrering.start

import kotliquery.TransactionalSession
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
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
import rapidsandrivers.SAK_ID_KEY
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.thread

class StartMigrering(
    val repository: StartMigreringRepository,
    val rapidsConnection: RapidsConnection,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        startMigrering()
    }

    fun startMigrering() {
        logger.info("Starter migrering for sakene som ligger i databasetabellen")
        val sakerTilMigrering =
            repository.hentSakerTilMigrering().distinct().also { logger.info("Migrerer ${it.size} saker") }
        repository.settSakerMigrert(sakerTilMigrering)
        if (sakerTilMigrering.isNotEmpty()) {
            thread {
                Thread.sleep(60_000)
                val sendTilMigrering = featureToggleService.isEnabled(MigreringFeatureToggle.SendSakTilMigrering, false)
                sakerTilMigrering.forEach {
                    rapidsConnection.publish(message = lagMelding(it), key = UUID.randomUUID().toString())
                    if (sendTilMigrering) {
                        Thread.sleep(3000)
                    }
                }
            }
        }
    }

    private fun lagMelding(sakTilMigrering: SakTilMigrering) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to Migreringshendelser.MIGRER_SPESIFIKK_SAK,
                SAK_ID_KEY to sakTilMigrering.sakId,
                LOPENDE_JANUAR_2024_KEY to true,
                MIGRERING_KJORING_VARIANT to sakTilMigrering.kjoringVariant,
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
                    "SELECT ${Databasetabell.SAKID}, ${Databasetabell.KJOERING} FROM ${Databasetabell.TABELLNAVN} " +
                        "WHERE ${Databasetabell.HAANDTERT} = FALSE",
            ) {
                SakTilMigrering(
                    sakId = it.long(Databasetabell.SAKID),
                    kjoringVariant = MigreringKjoringVariant.valueOf(it.string(Databasetabell.KJOERING)),
                )
            }
        }

    fun settSakerMigrert(
        saker: List<SakTilMigrering>,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query =
                "UPDATE ${Databasetabell.TABELLNAVN} SET ${Databasetabell.HAANDTERT} = true WHERE " +
                    "${Databasetabell.SAKID} = ANY(:saker)",
            params =
                mapOf(
                    "saker" to createArrayOf("bigint", saker.map { it.sakId }),
                ),
            loggtekst = "Markerte $saker som påstarta for migrering",
        )
    }

    internal object Databasetabell {
        const val TABELLNAVN = "saker_til_migrering"
        const val HAANDTERT = "haandtert"
        const val SAKID = "sakId"
        const val KJOERING = "kjoering"
    }
}

data class SakTilMigrering(val sakId: Long, val kjoringVariant: MigreringKjoringVariant)
