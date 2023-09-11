package no.nav.etterlatte.kafka

import no.nav.etterlatte.libs.common.logging.withLogContext
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

object KafkaRunner {
    fun <T> startLytting(
        konsument: Kafkakonsument<T>,
        closed: AtomicBoolean,
        logger: Logger
    ) {
        withLogContext {
            Thread {
                try {
                    konsument.stream()
                } catch (e: Exception) {
                    logger.error("App avsluttet med en feil", e)
                    exitProcess(1)
                }
            }.start()
        }
    }
}