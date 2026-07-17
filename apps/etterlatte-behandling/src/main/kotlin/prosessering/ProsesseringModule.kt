package no.nav.etterlatte.prosessering

import efterlatte.prosessering.ktor.Prosessering
import efterlatte.prosessering.ktor.taskProdusent
import efterlatte.prosessering.postgres.PostgresTaskRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("ProsesseringModule")

enum class ProsesseringToggles(
    private val toggle: String,
) : FeatureToggle {
    SKYGGE_SOEKNADMOTTAK("prosessering-soeknad-skygge"),
    PROSESSERING_ADMIN("prosessering-admin"),
    ;

    override fun key(): String = toggle
}

data class SoeknadSkyggeRequest(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

/**
 * Installerer prosessering-motoren på [Route.application]. Kalles kun fra
 * produksjons-oppsettet (ikke fra den delte `module`-testinngangen), slik at
 * motoren og reaperen ikke starter i den store mengden behandling-tester.
 *
 * Motoren poller `prosessering.task` og er inaktiv når ingen tasker finnes — det er
 * *produksjonen* av tasker som er gated bak [ProsesseringToggles.SKYGGE_SOEKNADMOTTAK]
 * (se [prosesseringSkyggeRoutes]).
 */
fun Route.installProsessering(
    dataSource: DataSource,
    node: String = System.getenv("HOSTNAME") ?: "etterlatte-behandling",
) {
    application.install(Prosessering) {
        repository = PostgresTaskRepository(dataSource)
        steg = listOf(soeknadMottakSkyggeSteg(), feilbarDemoSteg())
        this.node = node
    }
}

/**
 * Internt systembruker-endepunkt som legger en skyggekjørings-task i kø. Kalles av
 * en river i behandling-kafka når en søknad kommer inn (parallelt med dagens flyt).
 * Bruker `opprettFrittstående` fordi skyggen ikke har noe forretnings-skriv å henge
 * outbox-garantien på. Gated bak [ProsesseringToggles.SKYGGE_SOEKNADMOTTAK].
 */
fun Route.prosesseringSkyggeRoutes(featureToggleService: FeatureToggleService) {
    route("/api/prosessering/skygge/soeknad") {
        post {
            kunSystembruker {
                if (!featureToggleService.isEnabled(ProsesseringToggles.SKYGGE_SOEKNADMOTTAK, false)) {
                    logger.info("Skyggekjøring av søknadsmottak er slått av — hopper over")
                    call.respond(HttpStatusCode.NoContent)
                    return@kunSystembruker
                }

                val request = call.receive<SoeknadSkyggeRequest>()
                val taskId =
                    call.application.taskProdusent.opprettFrittstående(
                        type = soeknadMottakSkyggeType,
                        payload =
                            SoeknadMottakPayload(
                                soeknadId = request.soeknadId,
                                sakType = request.sakType,
                                fnrSoeker = request.fnrSoeker,
                            ),
                    )
                logger.info("La skyggekjørings-task ${taskId.verdi} i kø for søknad ${request.soeknadId}")
                call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}

/** Request for å opprette en [feilbarDemoType]-task. [vinduSekunder] styrer hvor lenge den simulerte
 * avhengigheten er «nede» — task-en feiler til dette vinduet har gått, og fullfører ved rekjøring etterpå. */
data class FeilbarDemoRequest(
    val vinduSekunder: Long = 20,
)

data class FeilbarDemoResponse(
    val taskId: Long,
    val simulertOppeFra: Instant,
)

/**
 * Operatør-/demo-endepunkt (PoC Fase 4d) som legger en [feilbarDemoType]-task i kø. Task-en feiler
 * mens den simulerte avhengigheten er «nede» (frem til `now + vinduSekunder`), går til STOPPET når
 * retriene er brukt opp, og fullfører når en operatør **rekjører** den etter at vinduet har gått.
 * Formålet er å demonstrere rekjøring-styrken i konseptet fra ende til annen.
 *
 * Saksbehandler-auth og gated bak [ProsesseringToggles.PROSESSERING_ADMIN] — samme operatør-flate
 * som innsyns- og rekjør-API-et ([prosesseringAdminRoutes]).
 */
fun Route.prosesseringDemoRoutes(featureToggleService: FeatureToggleService) {
    route("/api/prosessering/demo/feilbar") {
        post {
            kunSaksbehandler {
                if (!featureToggleService.isEnabled(ProsesseringToggles.PROSESSERING_ADMIN, false)) {
                    call.respond(HttpStatusCode.NotFound)
                    return@kunSaksbehandler
                }

                val request = call.receive<FeilbarDemoRequest>()
                val simulertOppeFra = Instant.now().plusSeconds(request.vinduSekunder)
                val payload =
                    FeilbarDemoPayload(
                        demoId = UUID.randomUUID().toString(),
                        simulertOppeFra = simulertOppeFra,
                    )
                val taskId =
                    call.application.taskProdusent.opprettFrittstående(
                        type = feilbarDemoType,
                        payload = payload,
                    )
                logger.info("La feilbar demo-task ${taskId.verdi} i kø (nede til $simulertOppeFra)")
                call.respond(HttpStatusCode.Accepted, FeilbarDemoResponse(taskId = taskId.verdi, simulertOppeFra = simulertOppeFra))
            }
        }
    }
}
