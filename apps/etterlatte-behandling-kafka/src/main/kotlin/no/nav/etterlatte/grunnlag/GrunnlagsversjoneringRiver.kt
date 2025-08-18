package no.nav.etterlatte.grunnlag

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagsversjoneringRiver(
    rapidsConnection: RapidsConnection,
    private val grunnlagKlient: GrunnlagKlient,
) : ListenerMedLogging() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
            validate { it.requireKey("vedtak.behandlingId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = UUID.fromString(packet["vedtak.behandlingId"].asText())

        try {
            logger.info("Mottok melding om attestert behandling (id=$behandlingId). Forsøker å låse grunnlagsversjon..")
            grunnlagKlient.laasVersjonForBehandling(behandlingId)
        } catch (e: Exception) {
            logger.error("Feil oppsto ved låsing av attestert behandling (id=$behandlingId): ", e)
        }
    }
}
