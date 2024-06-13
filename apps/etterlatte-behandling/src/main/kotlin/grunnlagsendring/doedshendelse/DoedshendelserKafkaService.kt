package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.ER_OVER_18_AAR
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

interface DoedshendelserKafkaService {
    fun sendBrevRequestOMS(
        sak: Sak,
        borIUtlandet: Boolean,
    )

    fun sendBrevRequestBP(
        sak: Sak,
        borIUtlandet: Boolean,
        erOver18aar: Boolean,
    )
}

class DoedshendelserKafkaServiceImpl(
    private val rapid: KafkaProdusent<String, String>,
) : DoedshendelserKafkaService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendBrevRequestOMS(
        sak: Sak,
        borIUtlandet: Boolean,
    ) {
        val brevmal: String = Brevkoder.OMS_INFORMASJON_DOEDSFALL.name
        val innhold =
            mapOf(
                BOR_I_UTLAND_KEY to borIUtlandet,
            )
        publiserHendelse(brevmal, innhold, sak)
    }

    override fun sendBrevRequestBP(
        sak: Sak,
        borIUtlandet: Boolean,
        erOver18aar: Boolean,
    ) {
        val brevmal: String = Brevkoder.BP_INFORMASJON_DOEDSFALL.name
        val innhold =
            mapOf(
                BOR_I_UTLAND_KEY to borIUtlandet,
                ER_OVER_18_AAR to erOver18aar,
            )
        publiserHendelse(brevmal, innhold, sak)
    }

    private fun publiserHendelse(
        brevmal: String,
        innhold: Map<String, Any>,
        sak: Sak,
    ) {
        val correlationId = getCorrelationId()
        val defaultInnhold =
            mapOf(
                SAK_ID_KEY to sak.id,
                TEKNISK_TID_KEY to LocalDateTime.now(),
                BREVMAL_RIVER_KEY to brevmal,
                CORRELATION_ID_KEY to correlationId,
            )
        val altInnhold = defaultInnhold + innhold
        rapid
            .publiser(
                brevmal,
                JsonMessage
                    .newMessage(
                        BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType(),
                        altInnhold,
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event ${BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType()} for sak ${sak.id}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }
}
