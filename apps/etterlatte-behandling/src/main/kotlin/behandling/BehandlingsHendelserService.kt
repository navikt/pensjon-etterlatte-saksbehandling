package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.BehandlingRiverKey
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable

enum class BehandlingHendelseType {
    OPPRETTET, AVBRUTT
}

interface BehandlingHendelserKafkaProducer {
    fun sendMeldingForHendelse(behandling: Behandling, hendelseType: BehandlingHendelseType)
}

class BehandlingsHendelserKafkaProducerImpl(
    private val rapid: KafkaProdusent<String, String>
) : BehandlingHendelserKafkaProducer {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMeldingForHendelse(behandling: Behandling, hendelseType: BehandlingHendelseType) {
        val correlationId = getCorrelationId()
        if (behandling.type == BehandlingType.REVURDERING && hendelseType == BehandlingHendelseType.OPPRETTET) {
            sendBehovForNyttGrunnlag(behandling)
        }
        rapid.publiser(
            behandling.id.toString(),
            JsonMessage.newMessage(
                "BEHANDLING:${hendelseType.name}",
                mapOf(
                    CORRELATION_ID_KEY to correlationId,
                    BehandlingRiverKey.behandlingObjectKey to behandling
                )
            ).toJson()
        ).also { (partition, offset) ->
            logger.info(
                "Posted event BEHANDLING:${hendelseType.name} for behandling ${behandling.id}" +
                    " to partiton $partition, offset $offset correlationid: $correlationId"
            )
        }
    }

    private fun sendBehovForNyttGrunnlag(behandling: Behandling) {
        grunnlagsbehov(behandling).forEach {
            rapid.publiser(behandling.id.toString(), it.toJson())
        }
    }

    private fun grunnlagsbehov(behandling: Behandling): List<JsonMessage> {
        fun behovForSoeker(fellesInfo: Map<String, Serializable>, sakType: SakType, fnr: String): JsonMessage {
            val rolle = when (sakType) {
                SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                SakType.BARNEPENSJON -> PersonRolle.BARN
            }
            return JsonMessage.newMessage(
                mapOf(BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1, "fnr" to fnr, "rolle" to rolle) + fellesInfo
            )
        }

        fun behovForAvdoede(fellesInfo: Map<String, Serializable>, fnr: List<String>): List<JsonMessage> {
            return fnr.map {
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1,
                        "rolle" to PersonRolle.AVDOED,
                        "fnr" to it
                    ) + fellesInfo
                )
            }
        }

        fun behovForGjenlevende(fellesInfo: Map<String, Serializable>, fnr: List<String>): List<JsonMessage> {
            return fnr.map {
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        "rolle" to PersonRolle.AVDOED,
                        "fnr" to it
                    ) + fellesInfo
                )
            }
        }

        val behandlingsData = mapOf(
            "sakId" to behandling.sak.id,
            "sakType" to behandling.sak.sakType
        )
        val persongalleri = behandling.persongalleri

        return listOf(behovForSoeker(behandlingsData, behandling.sak.sakType, persongalleri.soeker)) +
            behovForAvdoede(behandlingsData, persongalleri.avdoed) +
            behovForGjenlevende(behandlingsData, persongalleri.gjenlevende)
    }
}