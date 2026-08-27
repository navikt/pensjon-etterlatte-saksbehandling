package no.nav.etterlatte.prosessering

import com.fasterxml.jackson.module.kotlin.readValue
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import org.slf4j.LoggerFactory

/** Task-type for skyggekjøring av søknadsmottak. Ingen sideeffekter: validerer og logger, oppretter aldri en behandling. */
data class SoeknadMottakSkyggePayload(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

val SoeknadMottakSkygge: TaskType<SoeknadMottakSkyggePayload> =
    TaskType(
        navn = "SoeknadMottakSkygge",
        serialiser = { objectMapper.writeValueAsString(it) },
        deserialiser = { objectMapper.readValue(it) },
    )

class SoeknadMottakSkyggeTaskStep : TaskStep<SoeknadMottakSkyggePayload> {
    override val type = SoeknadMottakSkygge

    override fun utfor(kontekst: TaskKontekst<SoeknadMottakSkyggePayload>) {
        val payload = kontekst.payload
        krevGyldigFnr(payload.fnrSoeker)
        logger.info(
            "Ville opprettet behandling for sak av type ${payload.sakType} " +
                "(soeknadId=${payload.soeknadId}) — skyggekjøring, ingen sideeffekter",
        )
    }

    private fun krevGyldigFnr(fnr: String) {
        requireNotNull(Folkeregisteridentifikator.ofNullable(fnr)) {
            "fnrSoeker er ikke et gyldig fødselsnummer"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SoeknadMottakSkyggeTaskStep::class.java)
    }
}
