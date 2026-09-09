package no.nav.etterlatte.prosessering

import com.fasterxml.jackson.module.kotlin.readValue
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory
import tools.jackson.module.kotlin.readValue
import java.time.Instant

data class FeilbarDemoPayload(
    val demoId: String,
    val simulertOppeFra: Instant,
)

val FeilbarDemo: TaskType<FeilbarDemoPayload> =
    TaskType(
        navn = "FeilbarDemo",
        serialiser = { objectMapper.writeValueAsString(it) },
        deserialiser = { objectMapper.readValue(it) },
    )

class FeilbarDemoTaskStep : TaskStep<FeilbarDemoPayload> {
    override val type = FeilbarDemo

    override fun utfor(kontekst: TaskKontekst<FeilbarDemoPayload>) {
        val payload = kontekst.payload
        if (Instant.now().isBefore(payload.simulertOppeFra)) {
            throw IllegalStateException(
                "Simulert nedstrøms-avhengighet er nede (demoId=${payload.demoId}, " +
                    "oppe fra ${payload.simulertOppeFra}) — tasken feiler med vilje. Rekjør etter det tidspunktet.",
            )
        }
        logger.info(
            "Feilbar demo-task fullført (demoId=${payload.demoId}) — den simulerte avhengigheten er oppe igjen",
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FeilbarDemoTaskStep::class.java)
    }
}
