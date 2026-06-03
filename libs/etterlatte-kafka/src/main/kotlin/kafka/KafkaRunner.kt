package no.nav.etterlatte.kafka

import no.nav.etterlatte.libs.common.logging.withLogContext
import org.slf4j.Logger
import kotlin.system.exitProcess

fun <K, T> startLytting(
    konsument: Kafkakonsument<K, in T>,
    logger: Logger,
) {
    withLogContext {
        Thread {
            try {
                logger.info("Starter å lytte på ${konsument.topic}")
                konsument.start()
            } catch (e: Exception) {
                logger.error("App avsluttet med en feil", e)
                exitProcess(1)
            }
        }.start()
    }
}
