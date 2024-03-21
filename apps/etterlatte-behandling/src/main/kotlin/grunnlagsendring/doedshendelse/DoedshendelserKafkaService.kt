package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

interface DoedshendelserKafkaService {
    fun sendBrevRequest(
        sak: Sak,
        borIUtlandet: Boolean,
    )
}

class DoedshendelserKafkaServiceImpl(
    private val rapid: KafkaProdusent<String, String>,
) : DoedshendelserKafkaService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendBrevRequest(
        sak: Sak,
        borIUtlandet: Boolean,
    ) {
        val correlationId = getCorrelationId()

        val brevmal: String =
            when (sak.sakType) {
                SakType.BARNEPENSJON -> Brevkoder.BP_INFORMASJON_DOEDSFALL.name
                SakType.OMSTILLINGSSTOENAD -> Brevkoder.OMS_INFORMASJON_DOEDSFALL.name
            }
        rapid.publiser(
            brevmal,
            JsonMessage.newMessage(
                BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType(),
                mapOf(
                    CORRELATION_ID_KEY to correlationId,
                    TEKNISK_TID_KEY to LocalDateTime.now(),
                    SAK_ID_KEY to sak.id,
                    BREVMAL_RIVER_KEY to brevmal,
                    BOR_I_UTLAND_KEY to borIUtlandet,
                ),
            ).toJson(),
        ).also { (partition, offset) ->
            logger.info(
                "Posted event ${BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType()} for sak ${sak.id}" +
                    " to partiton $partition, offset $offset correlationid: $correlationId",
            )
        }
    }
}
