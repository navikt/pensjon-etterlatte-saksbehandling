package efterlatte.prosessering.ktor.skygge

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.taskSteg
import org.slf4j.LoggerFactory

/**
 * PoC-demonstrasjon (Fase 3): en host-definert, typed task-type for skyggekjøring av
 * søknadsmottak. Lever her i ktor-modulens testkilde med vilje — det er *vertens*
 * domene (BP/OMS, fnr, søknad), ikke gjenbrukbar prosessering-infra, så den skal
 * ikke ligge i biblioteks-main. Ved Fase 4 flyttes tilsvarende task-type inn i
 * `etterlatte-behandling` (host-en) og kobles på søknad-eventet.
 */
enum class SakType {
    BARNEPENSJON,
    OMSTILLINGSSTOENAD,
}

data class SoeknadMottakPayload(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

/**
 * Task-typen bærer sin egen (de)serialisering — her Jackson — slik at `core` forblir
 * framework-agnostisk. Verten velger serialiseringsteknologi, ikke biblioteket.
 */
val soeknadMottakSkyggeType: TaskType<SoeknadMottakPayload> =
    TaskType(
        navn = "SoeknadMottakSkygge",
        serialiser = { objectMapper.writeValueAsString(it) },
        deserialiser = { objectMapper.readValue(it) },
    )

/**
 * Steget som simulerer mottaket: validerer payload og *logger* «ville opprettet
 * behandling …» — ingen sideeffekter, ingen kall til `etterlatte-behandling`.
 *
 * Validering som feiler kaster en exception; motoren rutere da tasken til KLAR-retry
 * og til slutt STOPPET (FEIL) når forsøkene er brukt opp. Det er nettopp det
 * skyggekjøringen skal demonstrere: reliable, retryable, observerbar behandling.
 *
 * [observer] kalles etter vellykket validering — default logger, mens tester kan
 * injisere en observatør som fanger hva som «ville blitt opprettet».
 */
fun soeknadMottakSkyggeSteg(observer: (SoeknadMottakPayload) -> Unit = ::loggMottak): TaskStep<SoeknadMottakPayload> =
    taskSteg(soeknadMottakSkyggeType) { kontekst -> utforMottak(kontekst = kontekst, observer = observer) }

private fun utforMottak(
    kontekst: TaskKontekst<SoeknadMottakPayload>,
    observer: (SoeknadMottakPayload) -> Unit,
) {
    val payload = kontekst.payload
    valider(payload)
    observer(payload)
}

private fun valider(payload: SoeknadMottakPayload) {
    require(payload.soeknadId.isNotBlank()) { "soeknadId mangler" }
    require(payload.fnrSoeker.matches(Regex("\\d{11}"))) {
        "fnrSoeker må være 11 siffer, var «${maskerFnr(payload.fnrSoeker)}»"
    }
}

private fun loggMottak(payload: SoeknadMottakPayload) {
    log.info(
        "Ville opprettet behandling for sak av type ${payload.sakType} " +
            "(soeknadId=${payload.soeknadId}, fnr=${maskerFnr(payload.fnrSoeker)}) — skyggekjøring, ingen sideeffekter",
    )
}

private fun maskerFnr(fnr: String): String = if (fnr.length >= 6) fnr.take(6) + "*****" else "*****"

private val log = LoggerFactory.getLogger("SoeknadMottakSkygge")
