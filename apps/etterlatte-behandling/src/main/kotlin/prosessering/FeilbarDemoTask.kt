package no.nav.etterlatte.prosessering

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.taskSteg
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Feilbar demo-task (PoC Fase 4d). En task som *feiler* mens en simulert nedstrøms-avhengighet
 * er «nede», og *fullfører* når den er «oppe igjen». Formålet er å demonstrere den ene styrken
 * i prosessering-konseptet som rapids-and-rivers ikke gir alene: en task som stopper på en
 * **forbigående** feil kan **rekjøres** av en operatør og da gå helt til ende.
 *
 * Flyten en demo viser:
 *  1. Task opprettes med [FeilbarDemoPayload.simulertOppeFra] et lite stykke frem i tid.
 *  2. Motoren prøver og feiler (avhengigheten er «nede») til retriene er brukt opp → STOPPET.
 *  3. Operatøren venter til avhengigheten er «oppe» og trykker «Rekjør» → task går til FULLFØRT.
 *
 * Steget har **ingen sideeffekter** — det bare kaster (nede) eller logger (oppe). Poenget er å
 * bevise reliable/retryable/observerbar task-håndtering, ikke å gjøre noe ekte arbeid.
 */
data class FeilbarDemoPayload(
    val demoId: String,
    val simulertOppeFra: Instant,
)

val feilbarDemoType: TaskType<FeilbarDemoPayload> =
    TaskType(
        navn = "FeilbarDemo",
        serialiser = { objectMapper.writeValueAsString(it) },
        deserialiser = { objectMapper.readValue(it, FeilbarDemoPayload::class.java) },
    )

fun feilbarDemoSteg(observer: (FeilbarDemoPayload) -> Unit = ::loggFullfoert): TaskStep<FeilbarDemoPayload> =
    taskSteg(feilbarDemoType) { kontekst -> utfor(kontekst = kontekst, observer = observer) }

private fun utfor(
    kontekst: TaskKontekst<FeilbarDemoPayload>,
    observer: (FeilbarDemoPayload) -> Unit,
) {
    val payload = kontekst.payload
    if (Instant.now().isBefore(payload.simulertOppeFra)) {
        throw IllegalStateException(
            "Simulert nedstrøms-avhengighet er nede (demoId=${payload.demoId}, oppe fra=${payload.simulertOppeFra}) " +
                "— task feiler med vilje. Rekjør etter at avhengigheten er «oppe».",
        )
    }
    observer(payload)
}

private fun loggFullfoert(payload: FeilbarDemoPayload) {
    log.info("Feilbar demo-task fullført (demoId=${payload.demoId}) — den simulerte avhengigheten er «oppe» igjen")
}

private val log = LoggerFactory.getLogger("FeilbarDemoTask")
