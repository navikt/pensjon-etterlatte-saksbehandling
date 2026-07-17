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
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import org.slf4j.LoggerFactory
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
        steg = listOf(soeknadMottakSkyggeSteg())
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
