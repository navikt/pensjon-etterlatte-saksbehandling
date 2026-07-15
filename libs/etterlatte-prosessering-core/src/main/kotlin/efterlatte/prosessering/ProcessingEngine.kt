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
 * statusmaskinen: FULLFØRT ved suksess, KLAR (med backoff) ved feil som skal
 * prøves igjen, STOPPET når retries er brukt opp.
 */
class ProcessingEngine(
    private val repo: TaskRepository,
    private val node: String,
    private val handler: suspend (Task) -> Unit,
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
        try {
            handler(task)
            TaskStateMachine.krev(fra = Status.KJØRER, til = Status.FULLFØRT)
            withContext(Dispatchers.IO) { repo.markFullført(task.id) }
        } catch (e: Exception) {
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
    }

    suspend fun stop() {
        loop.cancelAndJoin()
        supervisor.cancelAndJoin()
    }
}
