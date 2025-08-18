package no.nav.etterlatte.beregningkafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.asUUID
import org.slf4j.LoggerFactory

class SjekkOmTidligAlderpensjonRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun kontekst() = Kontekst.INNTEKTSJUSTERING

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireValue("vedtak.sak.sakType", SakType.OMSTILLINGSSTOENAD.name) }
            validate {
                it.requireAny(
                    "vedtak.type",
                    listOf(VedtakType.INNVILGELSE.name, VedtakType.ENDRING.name),
                )
            }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["vedtak.behandlingId"].asUUID()
        logger.info("Mottatt sjekk om tidlig alderpensjon for $behandlingId")
        beregningService.haandterTidligAlderpensjon(behandlingId)
    }
}
