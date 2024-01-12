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
import rapidsandrivers.FNR_KEY
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.thread

class StartInformasjonsbrevgenereringRiver(
    private val repository: StartBrevgenereringRepository,
    private val rapidsConnection: RapidsConnection,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        startBrevgenerering()
    }

    private fun startBrevgenerering() {
        logger.info("Starter å generere informasjonsbrev for mottakerne som ligger i databasetabellen")
        val fnr =
            repository.hentFnrTilBrevgenerering().distinct().also { logger.info("Genererer ${it.size} brev") }
        repository.settBrevPaastarta(fnr)
        if (fnr.isNotEmpty()) {
            thread {
                Thread.sleep(60_000)
                val opprettBrev =
                    featureToggleService.isEnabled(InformasjonsbrevFeatureToggle.SendInformasjonsbrev, false)
                fnr.forEach {
                    rapidsConnection.publish(message = lagMelding(it), key = UUID.randomUUID().toString())
                    if (opprettBrev) {
                        Thread.sleep(3000)
                    }
                }
            }
        }
    }

    private fun lagMelding(fnrTilBrevgenerering: FnrTilBrevgenerering) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to BrevEventKeys.OPPRETT_BREV,
                FNR_KEY to fnrTilBrevgenerering.fnr,
                BrevEventKeys.BREVMAL_KEY to fnrTilBrevgenerering.brevmal.name,
                SAK_TYPE_KEY to fnrTilBrevgenerering.sakType.name,
            ),
        ).toJson()
}

class StartBrevgenereringRepository(private val dataSource: DataSource) : Transactions<StartBrevgenereringRepository> {
    override fun <R> inTransaction(block: StartBrevgenereringRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun hentFnrTilBrevgenerering(tx: TransactionalSession? = null) =
        tx.session {
            hentListe(
                queryString =
                    "SELECT ${Databasetabell.FNR}, ${Databasetabell.BREVMAL}, ${Databasetabell.SAKTYPE} " +
                        "FROM ${Databasetabell.TABELLNAVN} " +
                        "WHERE ${Databasetabell.HAANDTERT} = FALSE",
            ) {
                FnrTilBrevgenerering(
                    fnr = it.string(Databasetabell.FNR),
                    brevmal = EtterlatteBrevKode.valueOf(it.string(Databasetabell.BREVMAL)),
                    sakType = SakType.valueOf(it.string(Databasetabell.SAKTYPE)),
                )
            }
        }

    fun settBrevPaastarta(
        fnr: List<FnrTilBrevgenerering>,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query =
                "UPDATE ${Databasetabell.TABELLNAVN} SET ${Databasetabell.HAANDTERT} = true WHERE " +
                    "${Databasetabell.FNR} = ANY(:saker)",
            params =
                mapOf(
                    "fnr" to createArrayOf("string", fnr.map { it.fnr }),
                ),
            loggtekst = "Markerte $fnr som påstarta for brevgenerering",
        )
    }

    internal object Databasetabell {
        const val TABELLNAVN = "brev_til_generering"
        const val HAANDTERT = "haandtert"
        const val FNR = "fnr"
        const val BREVMAL = "brevmal"
        const val SAKTYPE = "saktype"
    }
}

data class FnrTilBrevgenerering(val fnr: String, val brevmal: EtterlatteBrevKode, val sakType: SakType)

enum class InformasjonsbrevFeatureToggle(private val key: String) : FeatureToggle {
    SendInformasjonsbrev("send-informasjonsbrev"),
    ;

    override fun key() = key
}
