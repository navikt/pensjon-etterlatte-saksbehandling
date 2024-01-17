package no.nav.etterlatte.rivers

import kotliquery.TransactionalSession
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.BrevEventKeys
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.FNR_KEY
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.thread

class StartInformasjonsbrevgenereringRiver(
    private val repository: StartBrevgenereringRepository,
    private val rapidsConnection: RapidsConnection,
    private val featureToggleService: FeatureToggleService,
    private val sleep: (millis: Long) -> Unit = { Thread.sleep(it) },
    private val iTraad: (handling: (() -> Unit)) -> Unit = { thread { it() } },
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        // startBrevgenerering()
    }

    private fun startBrevgenerering() {
        logger.info("Starter å generere informasjonsbrev for mottakerne som ligger i databasetabellen")
        val brevAaOpprette =
            repository.hentTilBrevgenerering().distinct().also { logger.info("Genererer ${it.size} brev") }
        repository.settBrevPaastarta(brevAaOpprette)
        if (brevAaOpprette.isEmpty()) {
            return
        }
        iTraad {
            sleep(60_000)
            val opprettBrev =
                featureToggleService.isEnabled(InformasjonsbrevFeatureToggle.SendInformasjonsbrev, false)
            brevAaOpprette.forEach {
                if (opprettBrev) {
                    rapidsConnection.publish(message = lagMelding(it), key = UUID.randomUUID().toString())
                    sleep(3000)
                }
            }
        }
    }

    private fun lagMelding(brevgenereringRequest: BrevgenereringRequest) =
        JsonMessage.newMessage(
            listOf(
                EVENT_NAME_KEY to BrevEventKeys.OPPRETT_BREV,
                FNR_KEY to (brevgenereringRequest.fnr),
                BEHANDLING_ID_KEY to (brevgenereringRequest.behandlingId?.toString()),
                BrevEventKeys.BREVMAL_KEY to brevgenereringRequest.brevmal.name,
                SAK_TYPE_KEY to brevgenereringRequest.sakType.name,
            )
                .filter { it.second != null }
                .associate { it.first to it.second!! },
        ).toJson()
}

class StartBrevgenereringRepository(private val dataSource: DataSource) : Transactions<StartBrevgenereringRepository> {
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
                    brevmal = EtterlatteBrevKode.valueOf(it.string(Databasetabell.BREVMAL)),
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
    val brevmal: EtterlatteBrevKode,
    val sakType: SakType,
) {
    init {
        require(fnr != null || behandlingId != null) {
            "Enten fnr eller behandlingId må være definert"
        }
    }
}

enum class InformasjonsbrevFeatureToggle(private val key: String) : FeatureToggle {
    SendInformasjonsbrev("send-informasjonsbrev"),
    ;

    override fun key() = key
}
