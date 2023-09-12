package no.nav.etterlatte.kafka

import no.nav.etterlatte.libs.common.logging.withLogContext
import org.slf4j.Logger
import kotlin.system.exitProcess

fun <T> startLytting(
    konsument: Kafkakonsument<T>,
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