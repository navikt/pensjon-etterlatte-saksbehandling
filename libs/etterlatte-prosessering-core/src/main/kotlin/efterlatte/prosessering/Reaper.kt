package efterlatte.prosessering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tar tilbake tasker som ble stående i [Status.KJØRER] fordi poden som plukket
 * dem døde midt i et steg (crash, OOM, deploy). En task regnes som hengende når
 * [Task.plukketTid] er eldre enn [hengendeTimeout]; den settes da tilbake til
 * [Status.KLAR] og plukkes på nytt av en engine.
 *
 * Reaperen teller *ikke* dette som et vanlig retry (`antall_feil` røres ikke) —
 * en pod som dør er ikke det samme som at steget feilet. En task som konsekvent
 * dreper poden sin vil derfor kunne gjenopplives på ubestemt tid; det er en
 * bevisst avveining for PoC-en og noe et framtidig «dødt-brev»-signal kan fange.
 */
class Reaper(
    private val repo: TaskRepository,
    private val hengendeTimeout: Duration = 5.minutes,
    private val intervall: Duration = 30.seconds,
    private val naa: () -> Instant = Instant::now,
) {
    private val log = LoggerFactory.getLogger(Reaper::class.java)
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(supervisor + Dispatchers.IO)

    private lateinit var loop: Job

    fun start() {
        loop =
            scope.launch {
                while (isActive) {
                    gjenopprettEnGang()
                    delay(intervall)
                }
            }
    }

    /** Kjører én reaper-runde. Eksponert for test og for manuell/ad hoc-kjøring. */
    suspend fun gjenopprettEnGang(): Int {
        val grense = naa().minusMillis(hengendeTimeout.inWholeMilliseconds)
        val antall = withContext(Dispatchers.IO) { repo.gjenopprettHengende(plukketFoer = grense) }
        if (antall > 0) {
            log.warn("Reaper tok tilbake $antall hengende task(er) (KJØRER -> KLAR)")
        }
        return antall
    }

    suspend fun stop() {
        loop.cancelAndJoin()
        supervisor.cancelAndJoin()
    }
}
