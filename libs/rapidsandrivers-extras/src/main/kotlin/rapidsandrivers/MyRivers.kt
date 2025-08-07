package rapidsandrivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class MyRiver1(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "my_event") }
                validate { it.requireKey("a_required_key") }
                // nested objects can be chained using "."
                validate { it.requireValue("nested.key", "works_as_well") }
            }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        /* fordi vi bruker precondition() på event_name kan vi trygt anta at meldingen
           er "my_event", og at det er minst én av de ulike validate() som har feilet */
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        println(packet["a_required_key"].asText())
        // nested objects can be chained using "."
        println(packet["nested.key"].asText())
    }
}
