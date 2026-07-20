package no.nav.etterlatte.prosessering

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.taskSteg
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory

/**
 * Ekte outbox-kobling av søknadsmottak (PoC Fase 4e, Steg 2). Til forskjell fra
 * [soeknadMottakSkyggeType] — som legges i kø *frittstående* (uten forretnings-skriv) fra en
 * mutasjonsfri river — opprettes denne task-en **i samme transaksjon som behandlings-skrivet**
 * via [opprettPaaAktivBehandlingstransaksjon]. Det er selve outbox-garantien i praksis: task-raden
 * lever og dør sammen med behandling-raden.
 *
 * Nøkkelen er [behandlingId] (én behandling = én task), ikke `soeknadId`: behandlingsopprettelse er
 * allerede guardet (én åpen behandling per sak) og task-en henger på samme tx, så vi trenger ikke
 * den egne dedupe-sjekken skyggeflyten har (jf. [SoeknadSkyggeDao]).
 *
 * Steget er foreløpig **effektfritt** (Steg 2a): det logger «behandling X opprettet — ville
 * varslet …» uten sideeffekter. En ekte, idempotent sideeffekt kommer i Steg 2b.
 */
data class EkteBehandlingMottakPayload(
    val behandlingId: String,
    val sakId: Long,
    val sakType: SakType,
)

val ekteBehandlingMottakType: TaskType<EkteBehandlingMottakPayload> =
    TaskType(
        navn = "EkteBehandlingMottak",
        serialiser = { objectMapper.writeValueAsString(it) },
        deserialiser = { objectMapper.readValue(it, EkteBehandlingMottakPayload::class.java) },
    )

fun ekteBehandlingMottakSteg(observer: (EkteBehandlingMottakPayload) -> Unit = ::loggEkteMottak): TaskStep<EkteBehandlingMottakPayload> =
    taskSteg(ekteBehandlingMottakType) { kontekst -> utforEkteMottak(kontekst = kontekst, observer = observer) }

private fun utforEkteMottak(
    kontekst: TaskKontekst<EkteBehandlingMottakPayload>,
    observer: (EkteBehandlingMottakPayload) -> Unit,
) {
    val payload = kontekst.payload
    require(payload.behandlingId.isNotBlank()) { "behandlingId mangler" }
    observer(payload)
}

private fun loggEkteMottak(payload: EkteBehandlingMottakPayload) {
    log.info(
        "Behandling ${payload.behandlingId} opprettet (sak ${payload.sakId}, type ${payload.sakType}) " +
            "— ville varslet/registrert mottak (Steg 2a: ingen sideeffekter ennå)",
    )
}

private val log = LoggerFactory.getLogger("EkteBehandlingMottak")
