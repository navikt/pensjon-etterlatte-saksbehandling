package no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.omstillingsstoenad.Omstillingsstoenad
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class InnsendtSoeknadRiver(
    rapidsConnection: RapidsConnection
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(InnsendtSoeknadRiver::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(SoeknadInnsendt.eventName)
            correlationId()
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoTypeKey, SoeknadType.OMSTILLINGSSTOENAD.name) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoVersjonKey, "1") }
            validate { it.requireKey(SoeknadInnsendt.lagretSoeknadIdKey) }
            validate { it.requireKey(SoeknadInnsendt.hendelseGyldigTilKey) }
            validate { it.requireKey(SoeknadInnsendt.adressebeskyttelseKey) }
            validate { it.requireKey(SoeknadInnsendt.fnrSoekerKey) }
            validate { it.rejectKey(SoeknadInnsendt.dokarkivReturKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val soeknad = packet.soeknad()
        withLogContext(packet.correlationId) {
            logger.info("Mottatt søknad fra søker: ${soeknad.soeker.fornavn.svar}")
        }
    }

    private fun JsonMessage.soeknad() = this[FordelerFordelt.skjemaInfoKey].let {
        objectMapper.treeToValue<Omstillingsstoenad>(
            it
        )
    }
}