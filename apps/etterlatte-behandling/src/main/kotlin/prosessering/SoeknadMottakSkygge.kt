package no.nav.etterlatte.prosessering

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.taskSteg
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import org.slf4j.LoggerFactory

/**
 * Skyggekjøring av søknadsmottak (PoC Fase 4). Task-typen bor her i `etterlatte-behandling`
 * (host-en) fordi den er *vertens* domene — BP/OMS, fnr, søknad — ikke gjenbrukbar
 * prosessering-infra. Ved Fase 3 bodde en tilsvarende type i biblioteks-testkilden;
 * nå kobles den på det ekte søknad-eventet via en river i behandling-kafka.
 *
 * Steget har **ingen sideeffekter**: det validerer payloaden og *logger* «ville opprettet
 * behandling …». Det kaller aldri sak/behandling-opprettelse. Poenget er å bevise
 * reliable/retryable/observerbar task-håndtering, ikke å faktisk opprette behandling.
 */
data class SoeknadMottakPayload(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

val soeknadMottakSkyggeType: TaskType<SoeknadMottakPayload> =
    TaskType(
        navn = "SoeknadMottakSkygge",
        serialiser = { objectMapper.writeValueAsString(it) },
        deserialiser = { objectMapper.readValue(it, SoeknadMottakPayload::class.java) },
    )

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
    require(Folkeregisteridentifikator.isValid(payload.fnrSoeker)) {
        "fnrSoeker er ikke et gyldig fødselsnummer, var «${maskerFnr(payload.fnrSoeker)}»"
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
