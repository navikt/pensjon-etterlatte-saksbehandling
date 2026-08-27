package no.nav.etterlatte.prosessering

import com.fasterxml.jackson.module.kotlin.readValue
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Payload for demo-tasken. [simulertOppeFra] er tidspunktet den fiktive nedstrøms-avhengigheten
 * blir «oppe» igjen, og er med vilje lagt i payloaden framfor i en variabel i appen: payloaden
 * vises i prosessering-dashboardet, så den som skal rekjøre kan lese seg til når det er verdt å
 * prøve.
 */
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

/**
 * En task som feiler med vilje mens en simulert nedstrøms-avhengighet er «nede», og fullfører
 * når den er «oppe» igjen.
 *
 * Poenget er å ha noe å faktisk rekjøre. Skyggetaskene fullfører nesten alltid, og en task som
 * aldri stopper beviser ingenting om operatørflaten. Dette er den ene egenskapen prosessering
 * gir som rapids-and-rivers ikke gir alene: en forbigående feil parkerer arbeidet i stedet for å
 * miste det, og et menneske kan ta det opp igjen.
 *
 * Slik ser flyten ut:
 *  1. Tasken opprettes med [FeilbarDemoPayload.simulertOppeFra] et stykke fram i tid.
 *  2. Motoren prøver og feiler. Med standardinnstillingene (tre forsøk, 100–200 ms backoff) står
 *     den som `STOPPET` etter under ett sekund.
 *  3. Operatøren venter til vinduet har gått og trykker «Rekjør» i dashboardet — tasken går til
 *     `KLAR`, plukkes, og fullfører.
 *
 * Rekjører man for tidlig, feiler den én gang til og går rett tilbake til `STOPPET`. Det er en
 * følge av at `antall_feil` bevisst ikke nullstilles ved rekjør, og det er greit — også det er
 * verdt å se.
 *
 * Steget har ingen sideeffekter. Det kaster eller det logger.
 */
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
