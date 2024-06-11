package no.nav.etterlatte.rivers

import kotliquery.TransactionalSession
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.FNR_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.thread

class StartInformasjonsbrevgenereringRiver(
    private val repository: StartBrevgenereringRepository,
    private val rapidsConnection: RapidsConnection,
    private val sleep: (millis: Duration) -> Unit = { Thread.sleep(it) },
    private val iTraad: (handling: () -> Unit) -> Unit = { thread { it() } },
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun init() = startBrevgenerering()

    private fun startBrevgenerering() {
        logger.info("Starter å generere informasjonsbrev for mottakerne som ligger i databasetabellen")
        val brevAaOpprette =
            repository.hentTilBrevgenerering().distinct().also { logger.info("Genererer ${it.size} brev") }
        if (brevAaOpprette.isEmpty()) {
            return
        }
        repository.settBrevPaastarta(brevAaOpprette)
        iTraad {
            sleep(Duration.ofMinutes(1))
            logger.info("Starter informasjonsbrev-genereringa")
            brevAaOpprette.forEach {
                rapidsConnection.publish(message = lagMelding(it), key = UUID.randomUUID().toString())
                sleep(Duration.ofSeconds(3))
            }
            logger.info("Informasjonsbrev-generering ferdig")
        }
    }

    private fun lagMelding(brevgenereringRequest: BrevgenereringRequest) =
        JsonMessage
            .newMessage(
                listOf(
                    BrevRequestHendelseType.OPPRETT_BREV.lagParMedEventNameKey(),
                    FNR_KEY to (brevgenereringRequest.fnr),
                    BEHANDLING_ID_KEY to (brevgenereringRequest.behandlingId?.toString()),
                    BREVMAL_RIVER_KEY to brevgenereringRequest.brevmal.name,
                    SAK_TYPE_KEY to brevgenereringRequest.sakType.name,
                ).filter { it.second != null }
                    .associate { it.first to it.second!! },
            ).toJson()
}

class StartBrevgenereringRepository(
    private val dataSource: DataSource,
) : Transactions<StartBrevgenereringRepository> {
    override fun <R> inTransaction(block: StartBrevgenereringRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun hentTilBrevgenerering(tx: TransactionalSession? = null) =
        tx.session {
            hentListe(
                queryString =
                    "SELECT " +
                        "${Databasetabell.FNR}, " +
                        "${Databasetabell.BEHANDLING_ID}, " +
                        "${Databasetabell.BREVMAL}, " +
                        "${Databasetabell.SAKTYPE} " +
                        "FROM ${Databasetabell.TABELLNAVN} " +
                        "WHERE ${Databasetabell.HAANDTERT} = FALSE",
            ) {
                BrevgenereringRequest(
                    fnr = it.stringOrNull(Databasetabell.FNR),
                    brevmal = Brevkoder.valueOf(it.string(Databasetabell.BREVMAL)),
                    sakType = SakType.valueOf(it.string(Databasetabell.SAKTYPE)),
                    behandlingId = it.uuidOrNull(Databasetabell.BEHANDLING_ID),
                )
            }
        }

    fun settBrevPaastarta(
        brev: List<BrevgenereringRequest>,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query =
                "UPDATE ${Databasetabell.TABELLNAVN} SET ${Databasetabell.HAANDTERT} = true WHERE " +
                    "(${Databasetabell.FNR} = ANY(:fnr) OR ${Databasetabell.BEHANDLING_ID} = ANY(:behandling))",
            params =
                mapOf(
                    "fnr" to createArrayOf("text", brev.mapNotNull { it.fnr }),
                    "behandling" to createArrayOf("uuid", brev.mapNotNull { it.behandlingId }),
                ),
            loggtekst = "Markerte $brev som påstarta for brevgenerering",
        )
    }

    internal object Databasetabell {
        const val TABELLNAVN = "brev_til_generering"
        const val HAANDTERT = "haandtert"
        const val FNR = "fnr"
        const val BEHANDLING_ID = "behandling"
        const val BREVMAL = "brevmal"
        const val SAKTYPE = "saktype"
    }
}

data class BrevgenereringRequest(
    val fnr: String?,
    val behandlingId: UUID?,
    val brevmal: Brevkoder,
    val sakType: SakType,
) {
    init {
        require(fnr != null || behandlingId != null) {
            "Enten fnr eller behandlingId må være definert"
        }
    }
}
