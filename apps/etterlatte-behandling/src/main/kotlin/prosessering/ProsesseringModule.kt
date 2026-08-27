package no.nav.etterlatte.prosessering

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.Reaper
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.ktor.Prosessering
import efterlatte.prosessering.ktor.taskProdusent
import efterlatte.prosessering.postgres.PostgresTaskRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.clusterNavn
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.isDev
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

fun Application.installProsessering(dataSource: DataSource) {
    install(Prosessering) {
        repository = PostgresTaskRepository(dataSource)
        steg = listOfNotNull(SoeknadMottakSkyggeTaskStep(), FeilbarDemoTaskStep().takeIf { erDemomiljoe() })
        node = "etterlatte-behandling"
        reaperPaa = true
    }
}

/**
 * Demo-tasken og endepunktet som køer den hører hjemme i dev og lokalt, ikke i prod. Sjekken bor
 * ett sted slik at steget og ruta ikke kan komme i utakt: er ruta stengt, finnes det ingen tasker
 * av typen, og da er det ingen grunn til å ha steget registrert heller.
 */
private fun erDemomiljoe(): Boolean = !appIsInGCP() || isDev()

data class SoeknadSkyggeRequest(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

fun Route.soeknadSkyggeRoute(soeknadSkyggeDao: SoeknadSkyggeDao) {
    route("/api/prosessering/skygge/soeknad") {
        post {
            kunSystembruker {
                medBody<SoeknadSkyggeRequest> { request ->
                    if (soeknadSkyggeDao.harAlleredeHaandtertSoeknad(request.soeknadId)) {
                        call.respond(HttpStatusCode.OK, "allerede håndtert")
                        return@medBody
                    }
                    val taskProdusent: TaskProdusent = call.application.taskProdusent
                    taskProdusent.opprettIEgenTransaksjon(
                        SoeknadMottakSkygge,
                        SoeknadMottakSkyggePayload(
                            soeknadId = request.soeknadId,
                            sakType = request.sakType,
                            fnrSoeker = request.fnrSoeker,
                        ),
                    )
                    call.respond(HttpStatusCode.Created)
                }
            }
        }
    }
}

fun Route.prosesseringRoutes(
    prosesseringAdminDao: ProsesseringAdminDao,
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
) {
    route("/api/prosessering/task") {
        get {
            medProsesseringTilgang(saksbehandlerGroupIdsByKey) {
                val status =
                    call.request.queryParameters["status"]?.let {
                        runCatching { Status.valueOf(it) }.getOrNull()
                    }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                call.respond(prosesseringAdminDao.list(status = status, limit = limit))
            }
        }

        get("/{id}") {
            medProsesseringTilgang(saksbehandlerGroupIdsByKey) {
                val task = prosesseringAdminDao.finn(call.taskId())
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(task)
                }
            }
        }

        // Stiene er låst av kontrakten efterlatte-verktoy kaller
        // (.github/verktoy/03-prosessering-kontrakten.md) og er derfor ASCII-formen «rekjor».
        post("/{id}/rekjor") {
            medProsesseringTilgang(saksbehandlerGroupIdsByKey) { saksbehandler ->
                medBody<TaskHandling> { kropp ->
                    call.respond(
                        utfoerOgLogg(
                            prosesseringAdminDao = prosesseringAdminDao,
                            saksbehandler = saksbehandler,
                            id = call.taskId(),
                            forventetVersjon = kropp.versjon,
                            handling = OperatorHandling.REKJOER,
                        ),
                    )
                }
            }
        }

        post("/{id}/avbryt") {
            medProsesseringTilgang(saksbehandlerGroupIdsByKey) { saksbehandler ->
                medBody<TaskHandling> { kropp ->
                    call.respond(
                        utfoerOgLogg(
                            prosesseringAdminDao = prosesseringAdminDao,
                            saksbehandler = saksbehandler,
                            id = call.taskId(),
                            forventetVersjon = kropp.versjon,
                            handling = OperatorHandling.AVBRYT,
                        ),
                    )
                }
            }
        }
    }
}

/** Optimistisk lås. Uten den ville to operatører kunnet overskrive hverandre uoppdaget. */
data class TaskHandling(
    val versjon: Long,
)

data class FeilbarDemoRespons(
    val taskId: Long,
    val simulertOppeFra: Instant,
)

/**
 * Køer en [FeilbarDemo]-task så det finnes noe å faktisk rekjøre fra prosessering-dashboardet.
 * `vinduSekunder` styrer hvor lenge den simulerte avhengigheten er nede; tasken står som
 * `STOPPET` nesten umiddelbart, og fullfører ved rekjøring etter at vinduet har gått.
 *
 * Bare tilgjengelig i dev og lokalt — se [erDemomiljoe]. Vinduet tas som query-parameter og ikke
 * som kropp, slik at en `curl -X POST` uten mer seremoni er nok.
 */
fun Route.feilbarDemoRoute(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) {
    // Skrives ved oppstart. Er den ikke i Loki, kjører podden et image uten denne ruta — og da
    // er en 404 fra endepunktet et deploy-problem, ikke et kodeproblem.
    logger.info(
        "Registrerer POST /api/prosessering/demo/feilbar (cluster={}, demomiljø={})",
        clusterNavn() ?: "ukjent",
        erDemomiljoe(),
    )
    route("/api/prosessering/demo/feilbar") {
        post {
            // Eksplisitt kropp, ikke `call.respond(HttpStatusCode.NotFound)`. StatusPages skriver
            // om alle kroppsløse 404-er til «ruta er ikke konfigurert opp», og da blir «demoen er
            // avskrudd her» umulig å skille fra «ruta finnes ikke i imaget». Den forskjellen er
            // hele svaret når noen feilsøker fra dashboardet.
            if (!erDemomiljoe()) {
                throw IkkeFunnetException(
                    code = "DEMO_IKKE_TILGJENGELIG",
                    detail = "Feilbar demo-task finnes bare i dev og lokalt, ikke i ${clusterNavn() ?: "ukjent miljø"}",
                )
            }
            medProsesseringTilgang(saksbehandlerGroupIdsByKey) { saksbehandler ->
                val vinduSekunder = call.request.queryParameters["vinduSekunder"]?.toLongOrNull() ?: STANDARD_DEMOVINDU
                if (vinduSekunder !in 0..MAKS_DEMOVINDU) {
                    throw UgyldigForespoerselException(
                        code = "UGYLDIG_DEMOVINDU",
                        detail = "vinduSekunder må være mellom 0 og $MAKS_DEMOVINDU, var $vinduSekunder",
                    )
                }

                val simulertOppeFra = Instant.now().plusSeconds(vinduSekunder)
                val taskId =
                    call.application.taskProdusent.opprettIEgenTransaksjon(
                        type = FeilbarDemo,
                        payload =
                            FeilbarDemoPayload(
                                demoId = UUID.randomUUID().toString(),
                                simulertOppeFra = simulertOppeFra,
                            ),
                    )
                operatorlogg.info(
                    "{} opprettet feilbar demo-task {} — simulert avhengighet er nede til {}",
                    saksbehandler.ident(),
                    taskId.verdi,
                    simulertOppeFra,
                )
                call.respond(
                    HttpStatusCode.Created,
                    FeilbarDemoRespons(taskId = taskId.verdi, simulertOppeFra = simulertOppeFra),
                )
            }
        }
    }
}

private const val STANDARD_DEMOVINDU = 20L
private const val MAKS_DEMOVINDU = 3600L

/**
 * Prosessering-endepunktene er ikke saksbehandlingsflate, men en operatørinngang for
 * utviklere. Derfor kreves både et brukertoken — maskintoken avvises, en rekjøring må kunne
 * tilskrives et menneske — og den egne AD-rollen for prosessering.
 */
private suspend fun RoutingContext.medProsesseringTilgang(
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
    haandter: suspend (Saksbehandler) -> Unit,
) {
    kunSaksbehandler { saksbehandler ->
        if (!SaksbehandlerMedRoller(saksbehandler, saksbehandlerGroupIdsByKey).harRolleProsessering()) {
            call.respond(HttpStatusCode.Forbidden)
            return@kunSaksbehandler
        }
        haandter(saksbehandler)
    }
}

private fun ApplicationCall.taskId(): Long =
    krevIkkeNull(parameters["id"]?.toLongOrNull()) {
        "id mangler eller er ugyldig"
    }

/**
 * Tilgangslogging for de to skrivende handlingene.
 *
 * En rekjøring endrer produksjonstilstand, og «hvem rørte hva?» må kunne besvares i
 * ettertid. Loggingen ligger her og ikke i DAO-en fordi det er her identiteten finnes, og
 * den dekker alle utfall — også avviste forsøk, som er de interessante.
 *
 * Dette er ikke sporingslogg: en task er ikke et personoppslag. Det som logges er hvilken
 * ansatt som gjorde hva med hvilken task.
 */
private fun utfoerOgLogg(
    prosesseringAdminDao: ProsesseringAdminDao,
    saksbehandler: Saksbehandler,
    id: Long,
    forventetVersjon: Long,
    handling: OperatorHandling,
): Task =
    try {
        val task =
            when (handling) {
                OperatorHandling.REKJOER -> prosesseringAdminDao.rekjoer(id = id, forventetVersjon = forventetVersjon)
                OperatorHandling.AVBRYT -> prosesseringAdminDao.avbryt(id = id, forventetVersjon = forventetVersjon)
            }
        operatorlogg.info(
            "{} utførte {} på task {} — ny status {}",
            saksbehandler.ident(),
            handling,
            id,
            task.status,
        )
        task
    } catch (feil: Throwable) {
        operatorlogg.warn(
            "{} fikk avvist {} på task {}: {}",
            saksbehandler.ident(),
            handling,
            id,
            feil.message,
        )
        throw feil
    }

private val operatorlogg = LoggerFactory.getLogger("prosessering.operator")
private val logger = LoggerFactory.getLogger("no.nav.etterlatte.prosessering.ProsesseringModule")
