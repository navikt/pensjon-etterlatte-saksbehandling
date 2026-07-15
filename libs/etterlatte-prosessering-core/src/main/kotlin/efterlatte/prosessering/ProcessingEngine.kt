package efterlatte.prosessering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Coroutine-basert poll-og-kjør-motor.
 *
 * En supervisert løkke plukker en batch klare tasker ([TaskRepository.claimBatch]),
 * kjører hver på en avgrenset [Semaphore] for backpressure, og driver
 * statusmaskinen. Kjøringen er **transaksjon-per-steg**: hvert steg kjøres inne i
 * [TaskRepository.iEgenTransaksjon], og [TaskRepository.markerFullført] committer
 * atomisk sammen med stegets DB-skriv. Feiler steget, rulles tx tilbake og feilen
 * bokføres i en egen liten transaksjon — KLAR (med backoff) hvis det er retries
 * igjen, ellers STOPPET.
 *
 * Registrerte [TaskStep] slås opp på [TaskType.navn]; en task uten registrert steg
 * stoppes (konfigurasjonsfeil, ikke noe å prøve på nytt).
 */
class ProcessingEngine(
    private val repo: TaskRepository,
    private val produsent: TaskProdusent,
    steg: List<TaskStep<*>>,
    private val node: String,
    private val batchStorrelse: Int = 20,
    private val maxSamtidighet: Int = 8,
    private val pollIntervall: Duration = 50.milliseconds,
    private val maxAntallFeil: Int = 3,
    private val backoff: (Int) -> Duration = { forsok -> (forsok.toLong() * 100).milliseconds },
) {
    private val log = LoggerFactory.getLogger("ProcessingEngine[$node]")
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(supervisor + Dispatchers.IO)
    private val semaphore = Semaphore(maxSamtidighet)
    private val stegPerType: Map<String, TaskStep<*>> = steg.associateBy { it.type.navn }

    private lateinit var loop: Job

    fun start() {
        loop =
            scope.launch {
                while (isActive) {
                    val batch = withContext(Dispatchers.IO) { repo.claimBatch(batchStorrelse) }
                    if (batch.isEmpty()) {
                        delay(pollIntervall)
                        continue
                    }
                    batch
                        .map { task -> launch { semaphore.withPermit { behandle(task) } } }
                        .joinAll()
                }
            }
    }

    private suspend fun behandle(task: Task) {
        val steg = stegPerType[task.type]
        if (steg == null) {
            log.error("Ingen TaskStep registrert for type ${task.type} (task ${task.id}) -> STOPPET")
            withContext(Dispatchers.IO) {
                repo.markFeilet(
                    id = task.id,
                    nyStatus = Status.STOPPET,
                    stoppaarsak = Stoppaarsak.FEIL,
                    nesteTriggerTid = Instant.now(),
                )
            }
            return
        }
        try {
            withContext(Dispatchers.IO) {
                repo.iEgenTransaksjon { transaksjon -> kjorSteg(steg, task, transaksjon) }
            }
        } catch (e: Exception) {
            haandterFeil(task, e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun kjorSteg(
        steg: TaskStep<*>,
        task: Task,
        transaksjon: Transaksjon,
    ) {
        val typet = steg as TaskStep<Any>
        val payload = typet.type.deserialiser(task.payload ?: error("Task ${task.id} mangler payload for type ${task.type}"))
        val kontekst =
            TaskKontekst(
                task = task,
                payload = payload,
                transaksjon = transaksjon,
                produsent = produsent,
            )
        typet.utfor(kontekst)
        TaskStateMachine.krev(fra = Status.KJØRER, til = Status.FULLFØRT)
        repo.markerFullført(transaksjon = transaksjon, id = task.id)
    }

    private suspend fun haandterFeil(
        task: Task,
        e: Exception,
    ) {
        val nyttAntallFeil = task.antallFeil + 1
        if (nyttAntallFeil >= maxAntallFeil) {
            log.warn("Task ${task.id} feilet $nyttAntallFeil ganger -> STOPPET (FEIL)", e)
            TaskStateMachine.krev(fra = Status.KJØRER, til = Status.STOPPET)
            withContext(Dispatchers.IO) {
                repo.markFeilet(
                    id = task.id,
                    nyStatus = Status.STOPPET,
                    stoppaarsak = Stoppaarsak.FEIL,
                    nesteTriggerTid = Instant.now(),
                )
            }
        } else {
            val ventetid = backoff(nyttAntallFeil)
            log.info("Task ${task.id} feilet (forsøk $nyttAntallFeil) -> KLAR om $ventetid")
            TaskStateMachine.krev(fra = Status.KJØRER, til = Status.KLAR)
            withContext(Dispatchers.IO) {
                repo.markFeilet(
                    id = task.id,
                    nyStatus = Status.KLAR,
                    stoppaarsak = null,
                    nesteTriggerTid = Instant.now().plusMillis(ventetid.inWholeMilliseconds),
                )
            }
        }
    }

    suspend fun stop() {
        loop.cancelAndJoin()
        supervisor.cancelAndJoin()
    }
}
