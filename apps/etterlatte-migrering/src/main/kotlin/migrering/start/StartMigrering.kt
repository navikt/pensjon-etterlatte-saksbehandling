package no.nav.etterlatte.migrering.start

import kotliquery.TransactionalSession
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.thread

class StartMigrering(
    val repository: StartMigreringRepository,
    val rapidsConnection: RapidsConnection,
    private val featureToggleService: FeatureToggleService,
    private val iTraad: (handling: () -> Unit) -> Unit = { thread { it() } },
    private val sleep: (duration: Duration) -> Unit = { Thread.sleep(it) },
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
            iTraad {
                sleep(Duration.ofMinutes(1))
                val sendTilMigrering = featureToggleService.isEnabled(MigreringFeatureToggle.SendSakTilMigrering, false)
                sakerTilMigrering.forEach {
                    val message = lagMelding(it)
                    rapidsConnection.publish(message = message, key = UUID.randomUUID().toString())
                    if (sendTilMigrering) {
                        sleep(Duration.ofSeconds(3))
                    }
                }
            }
        }
    }

    private fun lagMelding(sakTilMigrering: SakTilMigrering) =
        JsonMessage.newMessage(
            mapOf(
                Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                SAK_ID_KEY to sakTilMigrering.sakId,
                LOPENDE_JANUAR_2024_KEY to sakTilMigrering.lopendeJanuar2024,
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
                    "SELECT ${Databasetabell.SAKID}, ${Databasetabell.KJOERING}, ${Databasetabell.LOPENDE} " +
                        "FROM ${Databasetabell.TABELLNAVN} " +
                        "WHERE ${Databasetabell.HAANDTERT} = FALSE",
            ) {
                SakTilMigrering(
                    sakId = it.long(Databasetabell.SAKID),
                    kjoringVariant = MigreringKjoringVariant.valueOf(it.string(Databasetabell.KJOERING)),
                    lopendeJanuar2024 = it.boolean(Databasetabell.LOPENDE),
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
        const val LOPENDE = "lopendeJanuar2024"
    }
}

data class SakTilMigrering(val sakId: Long, val kjoringVariant: MigreringKjoringVariant, val lopendeJanuar2024: Boolean)
