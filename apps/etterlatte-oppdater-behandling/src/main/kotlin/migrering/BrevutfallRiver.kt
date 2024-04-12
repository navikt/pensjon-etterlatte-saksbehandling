package no.nav.etterlatte.migrering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.EtterbetalingDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.Kontekst
import java.time.LocalDate
import java.time.Month

internal class BrevutfallRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Ventehendelser.TATT_AV_VENT_UNDER_20_SJEKKA) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun kontekst() = Kontekst.MIGRERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Legger inn brevutfall for behandling ${packet.behandlingId}")
        val request =
            BrevutfallOgEtterbetalingDto(
                behandlingId = packet.behandlingId,
                opphoer = false,
                etterbetaling =
                    EtterbetalingDto(
                        behandlingId = packet.behandlingId,
                        datoFom = LocalDate.of(2024, Month.JANUARY, 1),
                        datoTom = LocalDate.of(2024, Month.MARCH, 31),
                        kilde = Grunnlagsopplysning.Gjenoppretting.create(),
                    ),
                brevutfall =
                    BrevutfallDto(
                        behandlingId = packet.behandlingId,
                        aldersgruppe = Aldersgruppe.OVER_18,
                        lavEllerIngenInntekt = null,
                        feilutbetaling = null,
                        kilde = Grunnlagsopplysning.Gjenoppretting.create(),
                    ),
            )
        behandlingService.leggInnBrevutfall(request)
        logger.info("Brevutfall lagt inn for behandling ${packet.behandlingId}")
        packet.setEventNameForHendelseType(Ventehendelser.TATT_AV_VENT_UNDER_20_SJEKKA_OG_BREVUTFALL_LAGT_INN)
        context.publish(packet.toJson())
    }
}
