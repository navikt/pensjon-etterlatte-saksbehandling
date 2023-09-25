package no.nav.etterlatte.trygdetid.kafka

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.TRYGDETID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
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
import rapidsandrivers.migrering.ListenerMedLogging
import rapidsandrivers.withFeilhaandtering
import java.util.UUID

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val trygdetidService: TrygdetidService) :
    ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for migreringshendelser")
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.TRYGDETID)

            correlationId()
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(VILKAARSVURDERT_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
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

                    try {
                        // Oppretter alle trygdetidsperioder og beholder siste beregnede trygdetid
                        val beregnetTrygdetid =
                            request.trygdetid.perioder.map { periode ->
                                trygdetidService.beregnTrygdetidGrunnlag(behandlingId, tilGrunnlag(periode))
                            }.lastOrNull() ?: it.getOrThrow()

                        sendBeregnetTrygdetid(packet, beregnetTrygdetid, context, behandlingId)
                    } catch (e: Exception) {
                        logger.warn(
                            "Problem ved opprettelse av trygdetidsperioder for behandling $behandlingId - overstyrer beregnet trygdetid",
                            e,
                        )

                        // TODO - EY-2602 - flere felter trengs - feks utland
                        val detaljertBeregnetTrygdetid =
                            DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(
                                request.beregning.anvendtTrygdetid.toInt(),
                            )

                        val beregnetTrygdetid =
                            trygdetidService.overstyrBeregnetTrygdetid(
                                behandlingId,
                                detaljertBeregnetTrygdetid,
                            )

                        sendBeregnetTrygdetid(packet, beregnetTrygdetid, context, behandlingId)
                    }
                }
            }
    }

    private fun sendBeregnetTrygdetid(
        packet: JsonMessage,
        beregnetTrygdetid: TrygdetidDto,
        context: MessageContext,
        behandlingId: UUID,
    ) {
        packet[TRYGDETID_KEY] = beregnetTrygdetid.toJson()
        packet.eventName = Migreringshendelser.BEREGN
        context.publish(packet.toJson())
        logger.info(
            "Publiserte oppdatert migreringshendelse fra trygdetid for behandling $behandlingId",
        )
    }

    private fun tilGrunnlag(trygdetidsgrunnlag: Trygdetidsgrunnlag) =
        TrygdetidGrunnlagDto(
            id = null,
            type = TrygdetidType.FAKTISK.name,
            bosted = trygdetidsgrunnlag.landTreBokstaver,
            periodeFra = trygdetidsgrunnlag.datoFom.toLocalDate(),
            periodeTil = trygdetidsgrunnlag.datoTom.toLocalDate(),
            kilde =
                TrygdetidGrunnlagKildeDto(
                    tidspunkt = Tidspunkt.now().toString(),
                    ident = Vedtaksloesning.PESYS.name,
                ),
            beregnet = null,
            begrunnelse = "Migrert fra pesys",
            poengInnAar = trygdetidsgrunnlag.poengIInnAar,
            poengUtAar = trygdetidsgrunnlag.poengIUtAar,
            prorata = !trygdetidsgrunnlag.ikkeIProrata,
        )
}
