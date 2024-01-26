package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.MigrerSoekerRequest
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PERSONGALLERI_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.rapidsandrivers.migrering.persongalleri
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId
import java.util.UUID

class MigreringGrunnlagHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.LAGRE_GRUNNLAG) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(MIGRERING_GRUNNLAG_KEY) }
            validate { it.requireKey(PERSONGALLERI_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottok grunnlagshendelser for migrering")
        val sakId = packet.sakId
        val behandlingId = packet.behandlingId

        val request =
            objectMapper.treeToValue(packet[MIGRERING_GRUNNLAG_KEY], MigrerSoekerRequest::class.java)

        grunnlagService.lagreNyePersonopplysninger(
            sakId,
            behandlingId,
            Folkeregisteridentifikator.of(request.soeker),
            listOf(
                Grunnlagsopplysning(
                    UUID.randomUUID(),
                    Grunnlagsopplysning.Pesys.create(),
                    Opplysningstype.PERSONGALLERI_V1,
                    objectMapper.createObjectNode(),
                    packet.persongalleri.toJsonNode(),
                ),
            ),
        )
        grunnlagService.lagreNyeSaksopplysninger(
            sakId,
            behandlingId,
            listOf(
                Grunnlagsopplysning(
                    UUID.randomUUID(),
                    Grunnlagsopplysning.Pesys.create(),
                    Opplysningstype.SPRAAK,
                    objectMapper.createObjectNode(),
                    packet.hendelseData.spraak.toJsonNode(),
                ),
            ),
        )

        packet.eventName = Migreringshendelser.VILKAARSVURDER.lagEventnameForType()
        context.publish(packet.toJson())

        logger.info("Behandla grunnlagshendelser for migrering for sak $sakId")
    }
}
