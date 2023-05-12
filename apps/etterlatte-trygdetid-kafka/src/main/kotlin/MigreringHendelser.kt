package no.nav.etterlatte.trygdetid.kafka

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.rapidsandrivers.migrering.DatoPeriode
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.TRYGDETID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.withFeilhaandtering

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val trygdetidService: TrygdetidService) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for migreringshendelser")
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.TRYGDETID)

            correlationId()
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(VILKAARSVURDERT_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.TRYGDETID) {
                val behandlingId = packet.behandlingId
                logger.info("Mottatt trygdetid-migreringshendelse for behandling $behandlingId")
                trygdetidService.beregnTrygdetid(behandlingId).also {
                    logger.info("Oppretta trygdetid for behandling $behandlingId")
                }
            }.takeIf { it.isSuccess }
                ?.let {
                    withFeilhaandtering(packet, context, Migreringshendelser.TRYGDETID_GRUNNLAG) {
                        val behandlingId = packet.behandlingId
                        val request = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY], MigreringRequest::class.java)
                        logger.info("Oppretter grunnlag for trygdetid for $behandlingId")

                        request.trygdetidperioder.perioder.forEach {
                            val beregnetTrygdetid =
                                trygdetidService.beregnTrygdetidGrunnlag(behandlingId, tilGrunnlag(it))
                            packet[TRYGDETID_KEY] = beregnetTrygdetid.toJson()
                            packet.eventName = Migreringshendelser.BEREGN
                            context.publish(packet.toJson())
                            logger.info(
                                "Publiserte oppdatert migreringshendelse fra trygdetid for behandling $behandlingId"
                            )
                        }
                    }
                }
        }
    }

    private fun tilGrunnlag(periode: DatoPeriode) = TrygdetidGrunnlagDto(
        id = null,
        type = TrygdetidType.NASJONAL.name,
        bosted = "Pesys",
        periodeFra = periode.fom,
        periodeTil = periode.tom,
        kilde = TrygdetidGrunnlagKildeDto(
            tidspunkt = Tidspunkt.now().toString(),
            ident = Vedtaksloesning.PESYS.name
        ),
        beregnet = null
    )
}