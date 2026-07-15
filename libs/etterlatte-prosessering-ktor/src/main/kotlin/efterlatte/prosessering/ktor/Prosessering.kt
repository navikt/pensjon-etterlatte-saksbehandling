package efterlatte.prosessering.ktor

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.Reaper
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.TaskStep
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Konfigurasjon for [Prosessering]-pluginen. Verten kobler inn sitt eget
 * [repository] (adapteren mot dens DB) og sine [steg], og gir noden et [node]-navn
 * for logging/sporing. Motorens og reaperens øvrige innstillinger beholder sine
 * fornuftige standarder — pluginen skal være minimal å ta i bruk.
 */
class ProsesseringConfig {
    /** Adapteren mot vertens database. Må settes. */
    lateinit var repository: TaskRepository

    /** Stegene motoren kan kjøre, ett per [efterlatte.prosessering.TaskType]. */
    var steg: List<TaskStep<*>> = emptyList()

    /** Node-navn for logging og sporing (typisk pod-navn/hostname). */
    var node: String = "ukjent-node"

    /** Slå av reaperen (f.eks. i test der hengende tasker ikke er relevant). */
    var reaperPaa: Boolean = true
}

/**
 * Nøkkel som lar ruter/handlere hente ut [TaskProdusent] fra applikasjonen etter
 * at pluginen er installert, via [Application.taskProdusent].
 */
val ProsesseringProdusentKey: AttributeKey<TaskProdusent> = AttributeKey("ProsesseringProdusent")

/** Produsenten pluginen wirer opp — tilgjengelig etter `install(Prosessering)`. */
val Application.taskProdusent: TaskProdusent
    get() = attributes[ProsesseringProdusentKey]

/**
 * Ktor-plugin som knytter prosessering-motoren til applikasjonens livssyklus.
 *
 * Ved installasjon bygges [StandardTaskProdusent], [ProcessingEngine] og en
 * [Reaper] rundt det oppgitte [ProsesseringConfig.repository], og produsenten
 * eksponeres via [Application.taskProdusent].
 *
 * - `ApplicationStarted` → start motor (og reaper) slik at klare tasker plukkes.
 * - `ApplicationStopPreparing` → stopp begge pent (graceful shutdown), så en task
 *   som kjører får kjøre ferdig og ingen nye plukkes.
 *
 * Bevisst minimal: ingen REST-ruter installeres her. Motoren gjør arbeidet;
 * produsenten legger arbeid i kø.
 */
val Prosessering =
    createApplicationPlugin(name = "Prosessering", createConfiguration = ::ProsesseringConfig) {
        val log = LoggerFactory.getLogger("ProsesseringPlugin")

        val repository = pluginConfig.repository
        val produsent = StandardTaskProdusent(repo = repository)
        val engine =
            ProcessingEngine(
                repo = repository,
                produsent = produsent,
                steg = pluginConfig.steg,
                node = pluginConfig.node,
            )
        val reaper = if (pluginConfig.reaperPaa) Reaper(repo = repository) else null

        application.attributes.put(ProsesseringProdusentKey, produsent)

        application.monitor.subscribe(ApplicationStarted) {
            log.info("Starter prosessering-motoren (node=${pluginConfig.node}, ${pluginConfig.steg.size} steg)")
            engine.start()
            reaper?.start()
        }

        application.monitor.subscribe(ApplicationStopPreparing) {
            log.info("Stopper prosessering-motoren (node=${pluginConfig.node})")
            runBlocking {
                engine.stop()
                reaper?.stop()
            }
        }
    }
